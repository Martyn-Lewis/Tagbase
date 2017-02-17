# Tagbase

# But what does it do?
It's a parser that takes simple boolean expressions (such as "a ? b : (c | d | (b e))") that act on tags (which are attached to objects) and builds them into a composed function.

This composed function can then be applied to an object (or a list of objects with filter) that inherits from Taggable and returns true or false if it matches.

The profiler method was just the result of curiosity, though it does reach a fairly decent ~60mn items per second on my system.

## After commit 577bdd
Commit 577bdd added context-free (produces a query that is objectively improved regardless of the dataset it's applied to) and contextual optimisations (produces a query that is subjectively improved only for the dataset provided).

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