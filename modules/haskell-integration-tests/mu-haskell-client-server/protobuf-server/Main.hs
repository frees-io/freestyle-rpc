{-# language DataKinds             #-}
{-# language FlexibleContexts      #-}
{-# language PartialTypeSignatures #-}
{-# language OverloadedLabels      #-}
{-# language OverloadedStrings     #-}
{-# language TypeApplications      #-}
{-# OPTIONS_GHC -fno-warn-partial-type-signatures #-}

module Main where

import           Data.Conduit
import qualified Data.Conduit.Combinators      as C
import           Data.Maybe                     ( fromMaybe )
import           Data.Monoid                   as M
import           Data.Text                     as T
                                                ( Text )
import           Control.Monad.IO.Class         ( liftIO )
import           Mu.GRpc.Server
import           Mu.Server
import           Mu.Schema.Optics

import           ProtobufProtocol

main :: IO ()
main = runGRpcApp msgProtoBuf 9123 server

ping :: (MonadServer m) => m ()
ping = return ()

getForecast :: (MonadServer m) => GetForecastRequest -> m GetForecastResponse
getForecast req = return resp
 where
  days      = fromIntegral $ fromMaybe 5 (req ^. #days_required)
  forecasts = sunnyDays days
  resp      = record (Just lastUpdated, Just forecasts)

lastUpdated :: T.Text
lastUpdated = "2020-03-20T12:00:00Z"

sunnyDays :: Int -> [Weather]
sunnyDays n = replicate n (enum @"SUNNY")

publishRainEvents
  :: (MonadServer m) => ConduitT () RainEvent m () -> m RainSummaryResponse
publishRainEvents source = toResponse <$> countRainStartedEvents
 where
  toResponse count = record1 $ Just (fromIntegral (M.getSum count))
  countRainStartedEvents = runConduit $ source .| C.foldMap countMsg
  countMsg msg = countEvent $ msg ^. #event_type
  countEvent (Just x) | x == started = M.Sum 1
  countEvent _                       = M.Sum 0

subscribeToRainEvents
  :: (MonadServer m)
  => SubscribeToRainEventsRequest
  -> ConduitT RainEvent Void m ()
  -> m ()
subscribeToRainEvents req sink = runConduit $ C.yieldMany events .| sink
 where
  events = toRainEvent <$> [started, stopped, started, stopped, started]
  toRainEvent x = record (city, Just x)
  city = req ^. #city

server :: (MonadServer m) => ServerT Maybe WeatherService m _
server = Server
  (     ping
  :<|>: getForecast
  :<|>: publishRainEvents
  :<|>: subscribeToRainEvents
  :<|>: H0
  )
