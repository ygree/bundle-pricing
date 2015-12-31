package xxx.bundlepricing.pricing

import org.scalatest.{Matchers, FlatSpec}
import xxx.bundlepricing.model._

class BruteForceCalculatorSpec extends FlatSpec with Matchers {

  import BruteForceCalculator._

  val apple = Item("Apple")
  val bread = Item("Bread")
  val margarine = Item("Margarine")

  def assertCombinations(expected: Set[Combination])(result: List[Combination]): Unit = {
    assertResult(expected)(result.toSet)
    assertResult(expected.size)(result.length)
  }

  "minCost" should "return min cost" in {
    val cart = Cart(Map(
      apple -> Qty(100),
      bread -> Qty(20)
    ))
    val applePrice = SimplePrice(apple, Qty(1), Cost(200))
    val secondAppleFree = BundlePrice(applePrice, SimplePrice(apple, Qty(1), Cost(0)))
    val threeCheaper = SimplePrice(apple, Qty(3), Cost(250))
    val breadPrice = SimplePrice(bread, Qty(1), Cost(500))
    val prices = List(applePrice, secondAppleFree, threeCheaper, breadPrice)

    val expected = Some(threeCheaper.cost * 32 + secondAppleFree.cost * 2 + breadPrice.cost * 20)
    val result = getCalculator(prices).minCost(cart)
    assertResult(expected)(result)
  }

  "minCost" should "perform well with lots of extra unrelated prices" in {
    val cart = Cart(Map(
      apple -> Qty(10)
    ))
    val applePrice = SimplePrice(apple, Qty(1), Cost(200))
    val unrelatedPrices = List.fill(50000)(SimplePrice(margarine, Qty(1), Cost(220)))
    val prices = unrelatedPrices :+ applePrice

    val expected = Some(applePrice.cost * 10)
    val result = getCalculator(prices).minCost(cart)
    assertResult(expected)(result)
  }

  "findAll" should "return simple fully match one price" in {
    val cart = Cart(Map(apple -> Qty(1)))
    val b = SimplePrice(apple, Qty(1), Cost(199))

    val result = findAll(List(b))(cart).toList

    assertCombinations(Set(Combination(Map(b -> 1), b.cost)))(result)
  }

  "findAll" should "return simple comb with factor 2" in {
    val cart = Cart(Map(apple -> Qty(2)))
    val b = SimplePrice(apple, Qty(1), Cost(199))

    val result = findAll(List(b))(cart).toList

    assertCombinations(Set(Combination(Map(b -> 2), b.cost * 2)))(result)
  }

  "findAll" should "return two simple comb with factor 1 and 2" in {
    val cart = Cart(Map(apple -> Qty(2)))
    val b1 = SimplePrice(apple, Qty(1), Cost(199))
    val b2 = SimplePrice(apple, Qty(2), Cost(215))

    val result = findAll(List(b1, b2))(cart).toList

    val expected = Set(
      Combination(Map(b1 -> 2), b1.cost * 2),
      Combination(Map(b2 -> 1), b2.cost)
    )
    assertCombinations(expected)(result)
  }

  "findAll" should "NOT include price if it's fully included in some other bundle price" in {
    val cart = Cart(Map(
      bread -> Qty(1),
      margarine -> Qty(2)
    ))
    val b3 = SimplePrice(margarine, Qty(1), Cost(110))
    val b2 = SimplePrice(bread, Qty(1), Cost(560))
    val b1 = BundlePrice(b2, b3, SimplePrice(margarine, Qty(1), Cost(0)))

    val result = findAll(List(b3, b1, b2))(cart).toList

    val expected = Set(
      Combination(Map(b1 -> 1), b1.cost),
      Combination(Map(b3 -> 2, b2 -> 1), b3.cost * 2 + b2.cost)
    )
    assertCombinations(expected)(result)
  }

  "findAll" should "NOT apply the same price more than once for the same position" in {
    val cart = Cart(Map(apple -> Qty(2)))
    val p = SimplePrice(apple, Qty(1), Cost(180))
    val b = BundlePrice(p, SimplePrice(apple, Qty(1), Cost(0)))

    val result = findAll(List(p, b))(cart).toList

    val expected = Set(
      Combination(Map(p -> 2), p.cost * 2),
      Combination(Map(b -> 1), b.cost)
    )
    assertCombinations(expected)(result)
  }

  "findAll" should "let price bundle price overlap" in {
    val peach = Item("peach")
    val cart = Cart(Map(apple -> Qty(2), peach -> Qty(1)))

    val apple1 = SimplePrice(apple, Qty(1), Cost(110))

    val appleSecond4Free = BundlePrice(apple1, SimplePrice(apple, Qty(1), Cost(0)))
    val peachCheaperWithApple = BundlePrice(apple1, SimplePrice(peach, Qty(1), Cost(215)))

    val result = findAll(List(
      appleSecond4Free,
      peachCheaperWithApple
    ))(cart).toList

    val expectedCost = appleSecond4Free.cost + peachCheaperWithApple.cost - apple1.cost
    val expected = Set(Combination(Map(appleSecond4Free -> 1, peachCheaperWithApple -> 1), expectedCost))
    assertCombinations(expected)(result)
  }

  "findAll" should "NOT let different price values overlap" in {
    val peach = Item("peach")
    val cart = Cart(Map(apple -> Qty(2), peach -> Qty(1)))

    val apple1a = SimplePrice(apple, Qty(1), Cost(110))
    val apple1b = SimplePrice(apple, Qty(1), Cost(120))

    val appleSecond4Free = BundlePrice(apple1a, SimplePrice(apple, Qty(1), Cost(0)))
    val peachCheaperWithApple = BundlePrice(apple1b, SimplePrice(peach, Qty(1), Cost(215)))

    val result = findAll(List(
      appleSecond4Free,
      peachCheaperWithApple
    ))(cart).toList

    assertResult(0)(result.length)
  }
}