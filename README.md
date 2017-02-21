# Tagbase

# Current progress

    Executing statement: INSERT INTO db1 VALUES {tags="a, b, c", value="Test row 1"}, {tags="a, b, c", value="Test row 2"}
    Executing statement: INSERT INTO db2 VALUES {tags="a, b, c", value="Test row 3"}, {tags="a, b, c", value="Test row 4"}
    Executing statement: SELECT * FROM (SELECT * FROM db1 WITH 'a' JOIN SELECT * FROM db2 WITH 'b') WITH 'a b'
    Database result: (a, b, c) Map(value -> "Test row 2")
    Database result: (a, b, c) Map(value -> "Test row 1")
    Database result: (a, b, c) Map(value -> "Test row 4")
    Database result: (a, b, c) Map(value -> "Test row 3")
    Executing statement: INSERT INTO my_directory VALUES {tags="photos, kittens", path="kitty.jpg", attributes="read-only"}
    Executing statement: INSERT INTO my_directory VALUES {tags="photos, dogs", path="puppy.jpg", attributes="read-only"}
    Executing statement: SELECT * FROM my_directory WITH 'photos'
    Database result: (photos, dogs) Map(path -> "puppy.jpg", attributes -> "read-only")
    Database result: (photos, kittens) Map(path -> "kitty.jpg", attributes -> "read-only")
    Executing statement: SELECT path FROM my_directory WITH 'dogs'
    Database result: (photos, dogs) Map(path -> "puppy.jpg")
    Executing statement: SELECT path, attributes FROM my_directory WITH 'kittens'
    Database result: (photos, kittens) Map(path -> "kitty.jpg", attributes -> "read-only")

# What does it do?
It's currently a parser that takes simple boolean expressions (such as "a ? b : (c | d | (b e))") that act on tags (which are attached to objects) and builds them into a composed function.

This composed function can then be applied to an object (or a list of objects with filter) that inherits from Taggable and returns true or false if it matches.

The profiler method was just the result of curiosity, though it does reach a fairly decent ~60mn items per second on my system.

## After query update
The query update added context-free (produces a query that is objectively improved regardless of the dataset it's applied to) and contextual optimisations (produces a query that is subjectively improved only for the dataset provided).

Further added was a parser for SELECT/JOIN/COUNT operations, where the input

    SELECT * FROM (SELECT * FROM db1 WITH 'a' JOIN SELECT * FROM db2 WITH 'b') WITH 'a b'

yields the expected results

    d6
    d5
    d4
    d3
    d2
    d6
    d5
    d4
    d3
    d2

from the expected parse tree

    Parsing query: SELECT * FROM (SELECT * FROM db1 WITH 'a' JOIN SELECT * FROM db2 WITH 'b') WITH 'a b'
    Multiple statements:
    	Select statement:
    		source:
    			subquery:
    				join:
    					Select statement:
    						source:
    							database reference: db1
    						typestring: *
    						expression: a
    					Select statement:
    						source:
    							database reference: db2
    						typestring: *
    						expression: b
    		typestring: *
    		expression: (a & b)

Some notable features of the optimisations

    Given a query such as a & b, if either a or b doesn't appear in the database, then execution will just yield nothing (nothing to do after all)

    Similarly, this effect cascades into the usual boolean optimisations. Consider a database that contains elements of a, but no b:
        a & b = a & never = never = do nothing
        a | b = a | never = a
        b | b = never | never = do nothing

    If a were always true instead (so all elements of the database contain a) then similar optimisations apply:
        a = always = do nothing (more accurately, return whatever is given)
        b | a = b | always = always = do nothing
        b & a = b

Perhaps more notably is that the code became a horrifying trainwreck past this point, as learning experiences are rarely ever pretty.