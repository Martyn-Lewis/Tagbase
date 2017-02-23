package datatypes

class CountResponse(val database: String, val results: Map[List[String], Int]) extends DatabaseResponse {

}
