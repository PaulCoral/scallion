package scallion

/** Contains definitions relating to regular expressions. */
trait RegExps[Character] {

  import RegExp._

  /** Regular expressions over characters. */
  sealed abstract class RegExp {

    /** Indicates if this regular expression accepts the empty word. */
    val acceptsEmpty: Boolean

    /** Returns the rest of a regular expression after consuming a character. */
    def derive(char: Character): RegExp = this match {
      case EmptyStr => EmptySet
      case EmptySet => EmptySet
      case Elem(predicate) =>
        if (predicate(char)) EmptyStr
        else EmptySet
      case Union(left, right) => left.derive(char) | right.derive(char)
      case Concat(first, second) =>
        if (first.acceptsEmpty) first.derive(char) ~ second | second.derive(char)
        else first.derive(char) ~ second
      case Star(regExp) => regExp.derive(char) ~ this
    }

    /** Union of `this` and `that` regular expression.
      *
      * Performs surface-level simplifications.
      *
      * @return A regular expression that accepts words if either `this` or `that` accepts it.
      */
    def |(that: RegExp): RegExp = (this, that) match {
      case (EmptySet, _) => that
      case (_, EmptySet) => this
      case _ => Union(this, that)
    }

    /** Concatenation of `this` and `that` regular expression.
      *
      * Performs surface-level simplifications.
      *
      * @return A regular expression that accepts words
      *.        if `this` accepts a prefix and `that` accepts the suffix.
     */
    def ~(that: RegExp): RegExp = (this, that) match {
      case (EmptySet, _) => EmptySet
      case (_, EmptySet) => EmptySet
      case (EmptyStr, _) => that
      case (_, EmptyStr) => this
      case _ => Concat(this, that)
    }

    /** Exactly `n` instances of `this` regular expression. */
    def times(n: Int): RegExp = {
      require(n >= 0)

      if (n == 0) {
        EmptyStr
      }
      else {
        this ~ this.times(n - 1)
      }
    }

    /** Zero or one instances of `this` regular expression. */
    def opt: RegExp = this | EmptyStr
  }

  /** Contains primitive constructors for regular expressions. */
  object RegExp {

    /** Accepts only the empty word. */
    case object EmptyStr extends RegExp {
      override val acceptsEmpty: Boolean = true
    }

    /** Never accepts. */
    case object EmptySet extends RegExp {
      override val acceptsEmpty: Boolean = false
    }

    /** Accepts single characters that satisfy a predicate. */
    case class Elem(predicate: Character => Boolean) extends RegExp {
      override val acceptsEmpty: Boolean = false
    }

    /** Union of two regular expressions. */
    case class Union(left: RegExp, right: RegExp) extends RegExp {
      override val acceptsEmpty: Boolean = left.acceptsEmpty || right.acceptsEmpty
    }

    /** Concatenation of two regular expressions. */
    case class Concat(first: RegExp, second: RegExp) extends RegExp {
      override val acceptsEmpty: Boolean = first.acceptsEmpty && second.acceptsEmpty
    }

    /** Kleene star of a regular expressions. */
    case class Star(regExp: RegExp) extends RegExp {
      override val acceptsEmpty: Boolean = true
    }
  }

  //---- Combinators ----//

  /** Regular expression that accepts any of the characters in `chars`. */
  def oneOf(chars: Seq[Character]): RegExp = {
    val set = Set(chars: _*)
    elem((c: Character) => set.contains(c))
  }

  /** Regular expression that accepts single characters based on a `predicate`. */
  def elem(predicate: Character => Boolean): RegExp = Elem(predicate)

  /** Regular expression that accepts only the single character `char`. */
  def elem(char: Character): RegExp = Elem(_ == char)

  /** Regular expression that accepts only the sequence of characters `chars`. */
  def word(chars: Seq[Character]): RegExp = {
    val empty: RegExp = EmptyStr
    chars.foldRight(empty) {
      case (char, rest) => elem(char) ~ rest
    }
  }

  /** Regular expression that accepts zero or more repetitions of `regExp`. */
  def many(regExp: RegExp): RegExp = Star(regExp)

  /** Regular expression that accepts one or more repetitions of `regExp`. */
  def many1(regExp: RegExp): RegExp = regExp ~ many(regExp)

  /** Regular expression that accepts zero or one instances of `regExp`. */
  def opt(regExp: RegExp): RegExp = regExp | EmptyStr
}

/* Regular expressions on characters. Expected to be mixed-in. */
trait CharRegExps { self: RegExps[Char] =>
  
  /** Single digit between 0 and 9. */
  val digit = elem(_.isDigit)

  /** Single digit between 1 and 9. */
  val nonZero = elem((c: Char) => c >= '1' && c <= '9')

  /** Single digit between 0 and 9 or A and F or a and f. */
  val hex = elem((c: Char) => c.isDigit || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))

  /** Single control character. */
  val control = elem(_.isControl)

  /** Single white space character. */
  val whiteSpace = elem(_.isWhitespace)
}