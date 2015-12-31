package xxx.bundlepricing.pricing

import xxx.bundlepricing.model.{Cost, Cart}

trait Calculator {
  def minCost(cart: Cart): Option[Cost]
}
