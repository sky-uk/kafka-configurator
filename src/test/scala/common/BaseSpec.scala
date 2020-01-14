package common

import org.scalatest.{EitherValues, FlatSpecLike, Matchers, TryValues}

abstract class BaseSpec extends FlatSpecLike with Matchers with TryValues with EitherValues