package datatypes

import parser.QueryObject

class InsertResponse(val database: String, val inserts: List[QueryObject]) extends DatabaseResponse {

}
