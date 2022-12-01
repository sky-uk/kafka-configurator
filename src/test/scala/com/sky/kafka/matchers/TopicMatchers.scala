package com.sky.kafka.matchers

import com.sky.kafka.configurator.Topic
import org.scalatest.Succeeded
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}

trait TopicMatchers {
  self: Matchers =>

  class TopicIsEquivalent(right: Topic) extends Matcher[Topic] {
    override def apply(left: Topic): MatchResult = {
      val name        = left.name == right.name
      val partitions  = left.partitions == right.partitions
      val replication = left.replicationFactor == right.replicationFactor
      val config      = (left.config should contain allElementsOf right.config).isInstanceOf[Succeeded.type]

      MatchResult(
        name && partitions && replication && config,
        s"$left is not equivalent to $right",
        s"$left is equivalent to $right"
      )
    }
  }

  def beEquivalentTo(topic: Topic) = new TopicIsEquivalent(topic)
}
