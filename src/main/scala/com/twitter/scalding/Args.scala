/*
Copyright 2012 Twitter, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.twitter.scalding

/**
* The args class does a simple command line parsing.  The rules are:
* keys start with one or more "-". Each key has zero or more values
* following.
*/
object Args {
  /**
  * Split on whitespace and then parse.
  */
  def apply(argString : String) : Args = Args(argString.split("\\s+"))
  /**
  * parses keys as starting with a dash. All following non-dashed args are a list of values.
  * If the list starts with non-dashed args, these are associated with the
  * empty string: ""
  **/
  def apply(args : Iterable[String]) : Args = {
    def startingDashes(word : String) = word.takeWhile { _ == '-' }.length
    new Args(
      //Fold into a list of (arg -> List[values])
        args.foldLeft(List("" -> List[String]())){(acc, arg) =>
        val noDashes = arg.dropWhile{ _ == '-'}
        if(arg == noDashes)
          (acc.head._1 -> (arg :: acc.head._2)) :: acc.tail
        else
          (noDashes -> List()) :: acc
      }. //Now reverse the values to keep the same order
      map {case (key, value) => key -> value.reverse}.toMap
    )
  }
}

@serializable
class Args(m : Map[String,List[String]]) {

  //Replace or add a given key+args pair:
  def +(keyvals : (String,Iterable[String])) = {
    new Args(m + (keyvals._1 -> keyvals._2.toList))
  }

  /**
  * Does this Args contain a given key?
  */
  def boolean(key : String) = m.contains(key)

  /**
  * Get the list of values associated with a given key.
  * if the key is absent, return the empty list.  NOTE: empty
  * does not mean the key is absent, it could be a key without
  * a value.  Use boolean() to check existence.
  */
  def list(key : String) = m.get(key).getOrElse(List())

  /**
  * This is a synonym for required
  */
  def apply(key : String) = required(key)

  /**
  * Equivalent to .optional(key).getOrElse(default)
  */
  def getOrElse(key : String, default : String) = optional(key).getOrElse(default)

  /**
  * return exactly one value for a given key.
  * If there is more than one value, you get an exception
  */
  def required(key : String) = list(key) match {
    case List() => error("Please provide a value for --" + key)
    case List(a) => a
    case _ => error("Please only provide a single value for --" + key)
  }

  /**
  * If there is zero or one element, return it as an Option.
  * If there is a list of more than one item, you get an error
  */
  def optional(key : String) : Option[String] = list(key) match {
    case List() => None
    case List(a) => Some(a)
    case _ => error("Please provide at most one value for --" + key)
  }
}