package xxx.bundlepricing

package object model {

  case class Cart(positions: Map[Item, Qty])

  case class Item(id: String) {
    override def toString = id
  }

  sealed trait Price {
    def cost: Cost

    def prices: Seq[SimplePrice]

    def includes(that: Price) = prices.contains(that)

    def positions: Map[Item, Qty] = prices.groupBy(_.item).mapValues(_.foldLeft(Qty(0))(_ + _.qty))

    def canBeFulfilledFrom(cart: Cart): Boolean = positions.forall {
      case (item, qty) => !(cart.positions.getOrElse(item, Qty(0)) < qty)
    }
  }

  case class SimplePrice(item: Item, qty: Qty, cost: Cost) extends Price {
    lazy val prices = Seq(this)
  }

  case class BundlePrice(prices: SimplePrice*) extends Price {
    lazy val cost = prices.map(_.cost).foldLeft(Cost(0))(_ + _)

    override def toString = prices.mkString("[", " + ", "]")
  }

  class Qty private(val pcs: Int) extends AnyVal {
    def +(that: Qty): Qty = Qty(this.pcs + that.pcs)

    def <(that: Qty): Boolean = this.pcs < that.pcs

    def -(that: Qty): Qty = Qty(this.pcs - that.pcs)

    def isZero = pcs == 0

    def min(that: Qty): Qty = Qty(Math.min(this.pcs, that.pcs))

    override def toString = s"$pcs pcs"
  }

  object Qty {
    def apply(pcs: Int): Qty = {
      assert(pcs >= 0)
      new Qty(pcs)
    }
  }

  class Cost private(val cents: Int) extends AnyVal {
    def *(factor: Int): Cost = Cost(cents * factor)

    def +(that: Cost): Cost = Cost(this.cents + that.cents)

    def -(that: Cost): Cost = Cost(this.cents - that.cents)

    def <(that: Cost): Boolean = this.cents < that.cents

    def min(that: Cost): Cost = Cost(Math.min(this.cents, that.cents))

    override def toString = s"$$${cents / 100}.${cents % 100}"
  }

  object Cost {
    def apply(cents: Int): Cost = new Cost(cents)
  }

}