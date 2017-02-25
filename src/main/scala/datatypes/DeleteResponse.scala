package datatypes

class DeleteResponse(val database: String, val expression: (DatabaseRow) => Boolean) extends DatabaseResponse {

}
