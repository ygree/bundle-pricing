package xxx.bundlepricing.pricing

import xxx.bundlepricing.model._

object BruteForceCalculator {

  def getCalculator(prices: List[Price]) = new Calculator {
    override def minCost(cart: Cart): Option[Cost] = {
      val combinations = findAll(prices)(cart)
      BruteForceCalculator.findCheapest(combinations).headOption.map(_.cost)
    }
  }

  case class Combination(prices: Map[Price, Int], cost: Cost)

  def findAll(prices: List[Price])(cart: Cart): Iterator[Combination] = {
    val onlyApplicablePrices = prices.filter(_.canBeFulfilledFrom(cart))
    findNext(onlyApplicablePrices, new PartialCombination(cart.positions, Nil, Cost(0)))
  }

  def findCheapest(combinations: TraversableOnce[Combination]): List[Combination] = {
    combinations.foldLeft(List[Combination]()) {
      case (firstCheapest :: _, combination) if combination.cost < firstCheapest.cost => List(combination)
      case (Nil, combination) => List(combination)
      case (cheapest, _) => cheapest
    }
  }

  private class PartialCombination(leftToBePriced: Map[Item, Qty], priced: List[Price], cost: Cost) {

    def tryPrice(price: Price): Option[PartialCombination] = {
      val newLeftToBePricedWithNewCost = price.prices.foldLeft(Option((leftToBePriced, cost))) {
        case (None, _) => None
        case (Some((toBePriced, acc)), subPrice) =>
          def tryFulfilPrice(inCartQty: Qty): Option[(Map[Item, Qty], Cost)] = {
            val notEnoughInCart = inCartQty < subPrice.qty
            if (notEnoughInCart) {
              val priceCanBeTakenAsPartOfSomeAlreadyPriced =
                priced.exists(b => b != price && b.includes(subPrice) && !subPrice.includes(b))
              if (!priceCanBeTakenAsPartOfSomeAlreadyPriced) None
              else Some((toBePriced, acc))
            }
            else {
              val leftToBePriced = toBePriced.updated(subPrice.item, inCartQty - subPrice.qty)
              val newCost = acc + subPrice.cost
              Some((leftToBePriced, newCost))
            }
          }
          toBePriced.get(subPrice.item) flatMap tryFulfilPrice
      }
      val redundantPriceExcluded = newLeftToBePricedWithNewCost.withFilter {
        case (_, newCost) => newCost != cost
      }
      redundantPriceExcluded.map {
        case (newLeftToBePriced, newCost) => new PartialCombination(newLeftToBePriced, price :: priced, newCost)
      }
    }

    def combination: Option[Combination] = {
      val notFullyPriced = leftToBePriced.exists {
        case (item, qty) => !qty.isZero
      }
      if (notFullyPriced) None
      else {
        val groupedPrices = priced.groupBy(p => p).mapValues(_.size)
        Some(Combination(groupedPrices, cost))
      }
    }
  }

  private def findNext(prices: List[Price], state: PartialCombination): Iterator[Combination] = {
    def tryOtherPrices: Iterator[Combination] = {
      prices match {
        case Nil => Iterator.empty
        case p :: ps =>
          def tryToSkip = findNext(ps, state)
          def tryOnceAndUtilPossible = {
            def tryOnce(nextStateOpt: Option[PartialCombination]): Iterator[Combination] = {
              nextStateOpt.map(nextState => tryOnce(nextState.tryPrice(p)) ++ findNext(ps, nextState)) getOrElse Iterator.empty
            }
            tryOnce(state.tryPrice(p))
          }
          tryOnceAndUtilPossible ++ tryToSkip
      }
    }
    state.combination.map(Iterator(_)) getOrElse tryOtherPrices
  }
}
