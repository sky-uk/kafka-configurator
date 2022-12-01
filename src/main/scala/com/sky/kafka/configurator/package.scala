package com.sky.kafka

import cats.data.WriterT
import cats.implicits._

import scala.util.{Failure, Success, Try}

package object configurator {

  type Logger[T] = WriterT[Try, Vector[String], T]

  implicit class TryLogger[T](private val t: Try[T]) extends AnyVal {

    def withLog(log: String): Logger[T] = t match {
      case Success(_) =>
        liftTryAndWrite(log)
      case Failure(_) =>
        t.asWriter
    }

    private def liftTryAndWrite(msg: String): Logger[T] =
      WriterT.putT(t)(Vector(msg))

    def asWriter: Logger[T] =
      WriterT.valueT(t)
  }

}
