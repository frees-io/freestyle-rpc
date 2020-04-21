/*
 * Copyright 2017-2020 47 Degrees <http://47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package higherkindness.mu.rpc.kafka

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.implicits._
import fs2.kafka._
import higherkindness.mu.rpc.protocol.{service, Empty}
import org.apache.kafka.common.{
  ConsumerGroupState => KConsumerGroupState,
  Node => KNode,
  TopicPartition => KTopicPartition,
  TopicPartitionInfo => KTopicPartitionInfo
}
import org.apache.kafka.common.acl.{AclOperation => KAclOperation}
import org.apache.kafka.common.config.{ConfigResource => KConfigResource}
import org.apache.kafka.clients.admin.{
  AlterConfigOp => KAlterConfigOp,
  ConfigEntry => KConfigEntry,
  ConsumerGroupDescription => KConsumerGroupDescription,
  ConsumerGroupListing => KConsumerGroupListing,
  MemberAssignment => KMemberAssignment,
  MemberDescription => KMemberDescription,
  TopicDescription => KTopicDescription,
  TopicListing => KTopicListing
}
import org.apache.kafka.clients.consumer.{OffsetAndMetadata => KOffsetAndMetadata}
import pbdirect.Pos

import scala.collection.JavaConverters._

object kafkaManagementService {
  final case class CreatePartitionsRequest(name: String, numPartitions: Int)

  final case class CreateTopicRequest(name: String, numPartitions: Int, replicationFactor: Short)
  final case class CreateTopicRequests(createTopicRequests: List[CreateTopicRequest])

  final case class DeleteTopicRequest(name: String)
  final case class DeleteTopicsRequest(names: List[String])

  final case class Node(id: Int, host: String, port: Int, rack: Option[String])
  object Node {
    def fromJava(n: KNode): Node = Node(n.id(), n.host(), n.port(), Option(n.rack()))
  }
  final case class Cluster(nodes: List[Node], controller: Node, clusterId: String)

  sealed trait ConfigType extends Pos
  object ConfigType {
    final case object TopicConfigType        extends ConfigType with Pos._0
    final case object BrokerConfigType       extends ConfigType with Pos._1
    final case object UnknownConfigType      extends ConfigType with Pos._2
    final case object BrokerLoggerConfigType extends ConfigType with Pos._3

    def toJava(ct: ConfigType): KConfigResource.Type = ct match {
      case TopicConfigType        => KConfigResource.Type.TOPIC
      case BrokerConfigType       => KConfigResource.Type.BROKER
      case UnknownConfigType      => KConfigResource.Type.UNKNOWN
      case BrokerLoggerConfigType => KConfigResource.Type.BROKER_LOGGER
    }
    def fromJava(kct: KConfigResource.Type): ConfigType = kct match {
      case KConfigResource.Type.TOPIC         => TopicConfigType
      case KConfigResource.Type.BROKER        => BrokerConfigType
      case KConfigResource.Type.UNKNOWN       => UnknownConfigType
      case KConfigResource.Type.BROKER_LOGGER => BrokerLoggerConfigType
    }
  }
  final case class ConfigResource(typ: ConfigType, name: String)
  object ConfigResource {
    def toJava(cr: ConfigResource): KConfigResource =
      new KConfigResource(ConfigType.toJava(cr.typ), cr.name)
    def fromJava(kcr: KConfigResource): ConfigResource =
      ConfigResource(ConfigType.fromJava(kcr.`type`()), kcr.name())
  }
  final case class DescribeConfigsRequest(resources: List[ConfigResource])
  sealed trait ConfigSource extends Pos
  object ConfigSource {
    final case object DynamicTopicConfig         extends ConfigSource with Pos._0
    final case object DynamicBrokerConfig        extends ConfigSource with Pos._1
    final case object DynamicDefaultBrokerConfig extends ConfigSource with Pos._2
    final case object StaticBrokerConfig         extends ConfigSource with Pos._3
    final case object DefaultConfig              extends ConfigSource with Pos._4
    final case object UnknownConfig              extends ConfigSource with Pos._5
    final case object DynamicBrokerLoggerConfig  extends ConfigSource with Pos._6

    def fromJava(kcs: KConfigEntry.ConfigSource): ConfigSource = kcs match {
      case KConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG          => DynamicTopicConfig
      case KConfigEntry.ConfigSource.DYNAMIC_BROKER_CONFIG         => DynamicBrokerConfig
      case KConfigEntry.ConfigSource.DYNAMIC_DEFAULT_BROKER_CONFIG => DynamicDefaultBrokerConfig
      case KConfigEntry.ConfigSource.STATIC_BROKER_CONFIG          => StaticBrokerConfig
      case KConfigEntry.ConfigSource.DEFAULT_CONFIG                => DefaultConfig
      case KConfigEntry.ConfigSource.UNKNOWN                       => UnknownConfig
      case KConfigEntry.ConfigSource.DYNAMIC_BROKER_LOGGER_CONFIG  => DynamicBrokerLoggerConfig
    }
  }
  final case class ConfigSynonym(name: String, value: String, source: ConfigSource)
  object ConfigSynonym {
    def fromJava(kcs: KConfigEntry.ConfigSynonym): ConfigSynonym =
      ConfigSynonym(kcs.name(), kcs.value(), ConfigSource.fromJava(kcs.source()))
  }
  final case class ConfigEntry(
      name: String,
      value: String,
      source: ConfigSource,
      isSensitive: Boolean,
      isReadOnly: Boolean,
      synonyms: List[ConfigSynonym]
  )
  object ConfigEntry {
    def fromJava(kce: KConfigEntry): ConfigEntry = ConfigEntry(
      kce.name(),
      kce.value(),
      ConfigSource.fromJava(kce.source()),
      kce.isSensitive(),
      kce.isReadOnly(),
      kce.synonyms().asScala.map(ConfigSynonym.fromJava).toList
    )
  }
  final case class Config(resource: ConfigResource, entries: List[ConfigEntry])
  final case class Configs(configs: List[Config])
  sealed trait OpType extends Pos
  object OpType {
    final case object Set      extends OpType with Pos._0
    final case object Delete   extends OpType with Pos._1
    final case object Append   extends OpType with Pos._2
    final case object Subtract extends OpType with Pos._3

    def toJava(ot: OpType): KAlterConfigOp.OpType = ot match {
      case Set      => KAlterConfigOp.OpType.SET
      case Delete   => KAlterConfigOp.OpType.DELETE
      case Append   => KAlterConfigOp.OpType.APPEND
      case Subtract => KAlterConfigOp.OpType.SUBTRACT
    }
  }
  final case class AlterConfigOp(name: String, value: String, opType: OpType)
  object AlterConfigOp {
    def toJava(aco: AlterConfigOp): KAlterConfigOp =
      new KAlterConfigOp(new KConfigEntry(aco.name, aco.value), OpType.toJava(aco.opType))
  }
  final case class AlterConfig(resource: ConfigResource, ops: List[AlterConfigOp])
  final case class AlterConfigsRequest(configs: List[AlterConfig])

  final case class TopicPartition(topic: String, partition: Int)
  object TopicPartition {
    def fromJava(ktp: KTopicPartition): TopicPartition =
      TopicPartition(ktp.topic(), ktp.partition())
  }
  final case class MemberAssignment(topicPartitions: List[TopicPartition])
  object MemberAssignment {
    def fromJava(kma: KMemberAssignment): MemberAssignment =
      MemberAssignment(kma.topicPartitions().asScala.map(TopicPartition.fromJava).toList)
  }
  final case class MemberDescription(
      consumerId: String,
      clientId: String,
      host: String,
      assignment: MemberAssignment
  )
  object MemberDescription {
    def fromJava(kmd: KMemberDescription): MemberDescription = MemberDescription(
      kmd.consumerId(),
      kmd.clientId(),
      kmd.host(),
      MemberAssignment.fromJava(kmd.assignment())
    )
  }
  sealed trait ConsumerGroupState extends Pos
  object ConsumerGroupState {
    final case object CompletingRebalance extends ConsumerGroupState with Pos._0
    final case object Dead                extends ConsumerGroupState with Pos._1
    final case object Empty               extends ConsumerGroupState with Pos._2
    final case object PreparingRebalance  extends ConsumerGroupState with Pos._3
    final case object Stable              extends ConsumerGroupState with Pos._4
    final case object Unknown             extends ConsumerGroupState with Pos._5

    def fromJava(kcgs: KConsumerGroupState): ConsumerGroupState = kcgs match {
      case KConsumerGroupState.COMPLETING_REBALANCE => CompletingRebalance
      case KConsumerGroupState.DEAD                 => Dead
      case KConsumerGroupState.EMPTY                => Empty
      case KConsumerGroupState.PREPARING_REBALANCE  => PreparingRebalance
      case KConsumerGroupState.STABLE               => Stable
      case KConsumerGroupState.UNKNOWN              => Unknown
    }
  }
  sealed trait AclOperation extends Pos
  object AclOperation {
    final case object All             extends AclOperation with Pos._0
    final case object Alter           extends AclOperation with Pos._1
    final case object AlterConfigs    extends AclOperation with Pos._2
    final case object Any             extends AclOperation with Pos._3
    final case object ClusterAction   extends AclOperation with Pos._4
    final case object Create          extends AclOperation with Pos._5
    final case object Delete          extends AclOperation with Pos._6
    final case object Describe        extends AclOperation with Pos._7
    final case object DescribeConfigs extends AclOperation with Pos._8
    final case object IdempotentWrite extends AclOperation with Pos._9
    final case object Read            extends AclOperation with Pos._10
    final case object Unknown         extends AclOperation with Pos._11
    final case object Write           extends AclOperation with Pos._12

    def fromJava(kao: KAclOperation): AclOperation = kao match {
      case KAclOperation.ALL              => All
      case KAclOperation.ALTER            => Alter
      case KAclOperation.ALTER_CONFIGS    => AlterConfigs
      case KAclOperation.ANY              => Any
      case KAclOperation.CLUSTER_ACTION   => ClusterAction
      case KAclOperation.CREATE           => Create
      case KAclOperation.DELETE           => Delete
      case KAclOperation.DESCRIBE         => Describe
      case KAclOperation.DESCRIBE_CONFIGS => DescribeConfigs
      case KAclOperation.IDEMPOTENT_WRITE => IdempotentWrite
      case KAclOperation.READ             => Read
      case KAclOperation.UNKNOWN          => Unknown
      case KAclOperation.WRITE            => Write
    }
  }
  final case class ConsumerGroupDescription(
      groupId: String,
      isSimpleConsumerGroup: Boolean,
      members: List[MemberDescription],
      partitionAssignor: String,
      state: ConsumerGroupState,
      coordinator: Node,
      authorizedOperations: List[AclOperation]
  )
  object ConsumerGroupDescription {
    def fromJava(kcgd: KConsumerGroupDescription): ConsumerGroupDescription =
      ConsumerGroupDescription(
        kcgd.groupId(),
        kcgd.isSimpleConsumerGroup(),
        kcgd.members().asScala.map(MemberDescription.fromJava).toList,
        kcgd.partitionAssignor(),
        ConsumerGroupState.fromJava(kcgd.state()),
        Node.fromJava(kcgd.coordinator()),
        kcgd.authorizedOperations().asScala.map(AclOperation.fromJava).toList
      )
  }
  final case class ConsumerGroup(groupId: String, description: ConsumerGroupDescription)
  final case class ConsumerGroups(consumerGroups: List[ConsumerGroup])
  final case class DescribeConsumerGroupsRequest(groupIds: List[String])

  final case class TopicPartitionInfo(
      partition: Int,
      leader: Node,
      replicats: List[Node],
      inSyncReplicas: List[Node]
  )
  object TopicPartitionInfo {
    def fromJava(ktpi: KTopicPartitionInfo): TopicPartitionInfo =
      TopicPartitionInfo(
        ktpi.partition(),
        Node.fromJava(ktpi.leader()),
        ktpi.replicas().asScala.map(Node.fromJava).toList,
        ktpi.isr().asScala.map(Node.fromJava).toList
      )
  }
  final case class TopicDescription(
      name: String,
      internal: Boolean,
      partitions: List[TopicPartitionInfo],
      authorizedOperations: List[AclOperation]
  )
  object TopicDescription {
    def fromJava(ktd: KTopicDescription): TopicDescription = TopicDescription(
      ktd.name(),
      ktd.isInternal(),
      ktd.partitions().asScala.map(TopicPartitionInfo.fromJava).toList,
      ktd.authorizedOperations().asScala.map(AclOperation.fromJava).toList
    )
  }
  final case class Topics(topics: List[TopicDescription])
  final case class DescribeTopicsRequest(names: List[String])

  final case class OffsetAndMetadata(
      offset: Long,
      metadata: String,
      leaderEpoch: Option[Int]
  )
  object OffsetAndMetadata {
    def fromJava(koam: KOffsetAndMetadata): OffsetAndMetadata =
      OffsetAndMetadata(
        koam.offset(),
        koam.metadata(),
        if (koam.leaderEpoch().isPresent()) Some(koam.leaderEpoch().get) else None
      )
  }
  final case class Offset(topicPartition: TopicPartition, metadata: OffsetAndMetadata)
  final case class ListConsumerGroupOffsetsRequest(groupId: String)
  final case class ConsumerGroupOffsets(offsets: List[Offset])

  final case class ConsumerGroupListing(
      groupId: String,
      isSimpleConsumerGroup: Boolean
  )
  object ConsumerGroupListing {
    def fromJava(kcgl: KConsumerGroupListing): ConsumerGroupListing = ConsumerGroupListing(
      kcgl.groupId(),
      kcgl.isSimpleConsumerGroup()
    )
  }
  final case class ConsumerGroupListings(consumerGroupListings: List[ConsumerGroupListing])

  final case class TopicListing(
      name: String,
      isInternal: Boolean
  )
  object TopicListing {
    def fromJava(ktl: KTopicListing): TopicListing = TopicListing(ktl.name(), ktl.isInternal())
  }
  final case class TopicListings(listings: List[TopicListing])

  @service(Protobuf)
  trait KafkaManagement[F[_]] {
    def alterConfigs(acr: AlterConfigsRequest): F[Unit]
    def createPartitions(cpr: CreatePartitionsRequest): F[Unit]
    def createTopic(ctr: CreateTopicRequest): F[Unit]
    def createTopics(ctrs: CreateTopicRequests): F[Unit]
    def deleteTopic(t: DeleteTopicRequest): F[Unit]
    def deleteTopics(ts: DeleteTopicsRequest): F[Unit]
    def describeCluster(r: Empty.type): F[Cluster]
    def describeConfigs(dcr: DescribeConfigsRequest): F[Configs]
    def describeConsumerGroups(dcgr: DescribeConsumerGroupsRequest): F[ConsumerGroups]
    def describeTopics(topics: DescribeTopicsRequest): F[Topics]
    def listConsumerGroupOffsets(lcgor: ListConsumerGroupOffsetsRequest): F[ConsumerGroupOffsets]
    def listConsumerGroups(r: Empty.type): F[ConsumerGroupListings]
    def listTopics(r: Empty.type): F[TopicListings]
  }

  object KafkaManagement {
    def buildInstance[F[_]: ContextShift: Concurrent](
        settings: AdminClientSettings[F]
    ): Resource[F, KafkaManagement[F]] =
      adminClientResource[F](settings)
        .map(new KafkaManagementImpl(_))
  }
}
