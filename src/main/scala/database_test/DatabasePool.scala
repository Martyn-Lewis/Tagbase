package database_test

class DatabasePool {
  var pools: collection.mutable.Map[String, Database] = collection.mutable.Map[String, Database]()

  def create_pool(name: String): Database = {
    if(!pools.contains(name)) pools += name -> new Database()
    get_pool(name)
  }
  def get_pool = pools(_)
}
