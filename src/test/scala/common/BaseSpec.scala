package common

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, TryValues}

abstract class BaseSpec extends AnyFlatSpecLike with Matchers with TryValues with EitherValues
