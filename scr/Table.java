
/****************************************************************************************
 * @file  Table.java
 *
 * @author   John Miller
 */

package relation;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.System.arraycopy;
import static java.lang.System.out;

/****************************************************************************************
 * The Table class implements relational database tables (including attribute names, domains
 * and a list of tuples.  Five basic relational algebra operators are provided: project,
 * select, union, minus and join.  The add/insert data manipulation operator is also provided.
 * Missing are update and delete data manipulation operators.
 */
public class Table
       implements Serializable
{
    /** Table name.
     */
    private final String name;

    /** Array of attribute names.
     */
    private final String [] attribute;

    /** Array of attribute domains: a domain may be
     *  integer types: Long, Integer, Short, Byte
     *  real types: Double, Float
     *  string types: Character, String
     */
    private final Class [] domain;

    /** Primary key (the attributes forming the PK). 
     */
    private final String [] key;

    /** Collection of tuples (data storage) stored in a List (e.g., ArrayList).
     */
    private final List <Comparable []> tuples;

    /** Index into tuples (maps key to tuple).
     */
    private final Map <KeyType, Comparable []> index;

    /** Map/Dictionary for mapping attribute names to column numbers
     */
    final HashMap <String, Integer> col;

    /** Counter for naming temporary tables.
     */
    private static int count = 0;

    /************************************************************************************
     * Write a flaw/error message.
     *
     * @param method   the method where the error occurred
     * @param message  the error message to be written
     * @return  false
     */
    public static boolean flaw (String method, String message)
    {
         out.println ("FLAW in " + method + ": " + message);
         return false;
    } // flaw

    /************************************************************************************
     * Make a map (index) of the appropriate type by COMMENTING OUT all but one.
     */
    private static Map <KeyType, Comparable []> makeMap ()
    {
        return new HashMap <> ();
//      return new TreeMap <> ();
    } // makeMap

    /************************************************************************************
     * Construct an empty table from the meta-data specifications.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     */  
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = new ArrayList <> ();
        index     = makeMap ();
        col       = new HashMap <> ();
        for (var i = 0; i < attribute.length; i++) col.put (attribute[i], i);
    } // constructor

    /************************************************************************************
     * Construct an empty table from the raw string specifications.
     *
     * @param _name       the name of the relation
     * @param attributes  the string containing attributes names
     * @param domains     the string containing attribute domains (data types)
     * @param _key        the primary key
     */
    public Table (String _name, String attributes, String domains, String _key)
    {
        this (_name, attributes.split (" "), findClass (domains.split (" ")), _key.split (" "));
    } // constructor

    /************************************************************************************
     * Construct a table from the meta-data specifications and data in the _tuples list.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     * @param _tuples     the list of tuples containing the data
     */
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key,
                  List <Comparable []> _tuples)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = _tuples;
        index     = makeMap ();
        col       = new HashMap <> ();
        for (var i = 0; i < attribute.length; i++) col.put (attribute[i], i);
    } // constructor

    /************************************************************************************
     * Find the classes in the "java.lang" package with given names.
     *
     * @param className  the array of class name (e.g., {"Integer", "String"})
     * @return  an array of Java classes
     */
    private static Class [] findClass (String [] className)
    {
        out.println ("findClass: className = " + Arrays.toString (className));
        var classArray = new Class [className.length];

        for (var i = 0; i < className.length; i++) {
            try {
                classArray[i] = Class.forName ("java.lang." + className[i]);
            } catch (ClassNotFoundException ex) {
                flaw ("findClass", ex.toString ());
            }
        } // for

        return classArray;
    } // findClass

    /************************************************************************************
     * Add a tuple to the table.
     *
     * @param tup  the array of attribute values forming the tuple
     * @return  the table, enables method chaining
     */
    public Table add (Comparable [] tup)
    {
         if (tup.length != attribute.length ) {
             flaw("add", "The arity of the input tuple does not match the arity of the table");
             return this;
         }

        for (int i = 0; i < domain.length; i++) {
            Class domainClass = domain[i];
            if (!(domainClass.isInstance(tup[i]))){
                flaw("add", String.format("The %s-th entry in the tuple is not in the domain", i));
                return this;
            }
         }

         tuples.add(tup);
         return this;
    } // add

    /************************************************************************************
     * Project the tuples onto a lower dimension by keeping only the given attributes.
     * Check whether the original key is included in the projection.
     * Use INDEX to efficiently eliminate DUPLICATE tuples.
     *
     * #usage deposit.proj ("bname cname")
     *
     * @param attrStr  the attributes (as a string) to project onto
     * @return  a table of projected tuples
     */
    public Table proj (String attrStr)
    {
        var attrs  = attrStr.split (" ");
        var newDom = getDom (attrs);
        var newKey = (Arrays.asList (attrs).containsAll (Arrays.asList (key))) ? key : attrs;

        List <Comparable []> rows = new ArrayList <> ();

        for (Comparable[] row : tuples) {
            Comparable[] newRow = new Comparable[attrs.length];
            for (int i = 0; i < attrs.length; i++) {
                newRow[i] = row[col.get(attrs[i])];
            }
            rows.add(newRow);
        }

        return new Table (name + count++, attrs, newDom, newKey, rows);
    } // proj

    /************************************************************************************
     * Get the domains corresponding the given attributes.
     *
     * @param attrs_ the attributes to project onto
     * @return  the corresponding domains
     */
    private Class [] getDom (String [] attrs)
    {
        out.println ("getDom: col = " + col);
        var obj = new Class [attrs.length];
        for (var j = 0; j < attrs.length; j++) {
            out.println ("getDom: attrs[j] = " + attrs[j]);
            obj[j] = domain [col.get (attrs[j])];
        } // for
        return obj;
    } // getDom

    /************************************************************************************
     * Select the tuples satisfying the given PREDICATE (Boolean function).
     *
     * #usage deposit.selt (t -> t[deposit.col.get ("bname")].equals ("Alps"))
     *
     * @param predicate  the check condition for tuples
     * @return  a table with tuples satisfying the predicate
     */
    public Table sel (Predicate <Comparable []> predicate)
    {
        return new Table (name + count++, attribute, domain, key,
                   tuples.stream ().filter (t -> predicate.test (t))
                                   .collect (Collectors.toList ()));
    } // sel

    /************************************************************************************
     * Select the tuples satisfying the given simple CONDITION on attributes/constants
     * compared using an <op> ==, !=, <, <=, >, >=.
     *
     * #usage deposit.selt ("bname == 'Alps'")
     *
     * @param condition  the check condition as a string for tuples
     * @return  a table with tuples satisfying the condition
     */
    public Table sel (String condition)
    {
        List <Comparable []> rows = new ArrayList <> ();

        var token = condition.split (" ");
        var colNo = col.get (token [0]);
        for (var t : tuples) {
            if (satisfies (t, colNo, token [1], token [2])) rows.add (t);
        } // for

        return new Table (name + count++, attribute, domain, key, rows);
    } // sel

    /************************************************************************************
     * Does tuple t satisfy the condition t[colNo] op value where op is ==, !=, <, <=, >, >=?
     *
     * #usage satisfies (t, 1, "<", "1980")
     *
     * @param colNo  the attribute's column number
     * @param op     the comparison operator
     * @param value  the value to compare with (must be converted, String -> domain type)
     * @return  whether the condition is satisfied
     */
    private boolean satisfies (Comparable [] t, int colNo, String op, String value)
    {
       Comparable tupleVal = t[colNo];
       Comparable compareVal;

       if (tupleVal instanceof Integer) {
              compareVal = Integer.valueOf(value);
       } else if (tupleVal instanceof Long) {
              compareVal = Long.valueOf(value);
       } else if (tupleVal instanceof Double) {
              compareVal = Double.valueOf(value);
       } else if (tupleVal instanceof Float) {
              compareVal = Float.valueOf(value);
       } else {
              compareVal = value.replace("'", "");
       }


       int cmp = tupleVal.compareTo(compareVal);

       switch(op) {
              case "==": return cmp == 0;
              case "!=": return cmp != 0;
              case "<": return cmp < 0;
              case ">": return cmp > 0;
              case "<=": return cmp <= 0;
              case ">=": return cmp >= 0;
              default: return false;
       }
    } // satisfies

    /************************************************************************************
     * Select the tuples satisfying the given key predicate (key = value).  Use an index
     * (Map) to retrieve the tuple with the given key value using the INDEXED SELECT algorithm.
     *
     * #usage deposit.sel (new KeyType ("Alps"))
     *
     * @param keyVal  the given key value
     * @return  a table with the tuple satisfying the key predicate
     */
    public Table sel (KeyType keyVal)
    {
       List <Comparable []> rows = new ArrayList <> ();

       Comparable[] tuple = index.get(keyVal);

       if (tuple != null) {
                  rows.add(tuple);
           }
       
       return new Table (name + count++, attribute, domain, key, rows);
    } // selt

    /************************************************************************************
     * Union this table and table2.  Check that the two tables are compatible.
     * Use INDEX to efficiently eliminate DUPLICATE tuples.
     *
     * #usage deposit.union (loan)
     *
     * @param table2  the rhs table in the union operation
     * @return  a table representing the union
     */
    public Table union (Table table2)
    {
        if (! compatible (table2)) return null;

        List <Comparable []> rows = new ArrayList <> ();
        Map <KeyType, Comparable []> tempIndex = makeMap ();

        for (Comparable [] t : tuples) {
            Comparable [] keyVal = new Comparable [key.length];

            for (int i = 0; i < key.length; i++) {
                keyVal [i] = t [col.get (key [i])];
            }

            KeyType k = new KeyType (keyVal);

            if (! tempIndex.containsKey (k)) {
                rows.add (t);
                tempIndex.put (k, t);
            }
        }

        for (Comparable [] t : table2.tuples) {
            Comparable [] keyVal = new Comparable [key.length];

            for (int i = 0; i < key.length; i++) {
                keyVal [i] = t [col.get (key [i])];
            }

            KeyType k = new KeyType (keyVal);

            if (! tempIndex.containsKey (k)) {
                rows.add (t);
                tempIndex.put (k, t);
            }
        }
        return new Table (name + count++, attribute, domain, key, rows);
    } // union

   /************************************************************************************
     * Determine whether the two tables (this and table2) are compatible, i.e., have
     * the same number of attributes each with the same corresponding domain.
     *
     * @param table2  the rhs table
     * @return  whether the two tables are compatible
     */
    private boolean compatible (Table table2)
    {
        if (domain.length != table2.domain.length)
            return flaw ("compatible", "tables have different arity");
        for (var j = 0; j < domain.length; j++) {
           if (domain [j] != table2.domain [j])
                return flaw ("compatible", "tables disagree on domain " + j);
        } // for
        return true;
    } // compatible

    /************************************************************************************
     * Take the difference of this table and table2.  Check that the two tables are compatible.
     * Use INDEX to speed up the minus operator.
     *
     * #usage deposit.minus (loan)
     *
     * @param table2  The rhs table in the minus operation
     * @return  a table representing the difference
     */
    public Table minus (Table table2)
    {
        if (! compatible (table2)) return null;

        List <Comparable []> rows = new ArrayList <> ();

        for (var t : tuples) {
            boolean found = false;
            for (var u : table2.tuples) {
                if (tupleCompare (t, u)) {
                    found = true;
                    break;
                }
            } // for
            if (! found) rows.add (t);
        } // for

        return new Table (name + count++, attribute, domain, key, rows);
    } // minus

    /************************************************************************************
     * Join this table and table2 by performing an EQUI-JOIN.  Tuples from both tables
     * are compared requiring attributes1 to equal attributes2.  Disambiguate attribute
     * names by appending "2" to the end of any duplicate attribute name.  Implement using
     * a Nested Loop Join Algorithm.
     *
     * #usage deposit.join ("cname", "cname", customer)
     *
     * @param attributes1  the attributes of this table to be compared (Foreign Key)
     * @param attributes2  the attributes of table2 to be compared (Primary Key)
     * @param table2       the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (String attributes1, String attributes2, Table table2)
    {   
        var t_attrs = attributes1.split (" ");
        var u_attrs = attributes2.split (" ");
        var rows    = new ArrayList <Comparable []> ();
        for (var t : this.tuples) {
            for (var u : table2.tuples) {
                boolean match = true;
                for (int i = 0; i < t_attrs.length; i++) {
                    var tVal = t[this.col.get(t_attrs[i])];
                    var uVal = u[table2.col.get(u_attrs[i])];
                    if (!tVal.equals(uVal)) {
                        match = false;                       
                    }
                }
                if (match) {
                    rows.add(concat(t,u));
                }
            }
        }
        
        return new Table (name + count++, concat (attribute, table2.attribute),
                                          concat (domain, table2.domain), key, rows);
    } // join

    /************************************************************************************
     * Join this table and table2 by performing a THETA-JOIN.  Tuples from both tables
     * are compared attribute1 <op> attribute2.  Disambiguate attribute names by appending "2"
     * to the end of any duplicate attribute name.  Implement using a Nested Loop Join algorithm.
     *
     * #usage deposit.join ("cname == cname", customer)
     *
     * @param condition  the theta join condition
     * @param table2     the rhs table in the join operation
     * @return  a table with tuples satisfying the condition
     */
    public Table join (String condition, Table table2)
    {   
        var rows = new ArrayList <Comparable []> ();
        String[] args = condition.split(" ");
        for (var t : this.tuples) {
            for (var u : table2.tuples) {
                var tVal = t[this.col.get(args[0])];
                var uVal = u[table2.col.get(args[2])];

                boolean match = switch (args[1]) {
                    case "==" -> tVal.compareTo(uVal) == 0;
                    case "!=" -> tVal.compareTo(uVal) != 0;
                    case "<" -> tVal.compareTo(uVal) < 0;
                    case "<=" -> tVal.compareTo(uVal) <= 0;
                    case ">" -> tVal.compareTo(uVal) > 0;
                    case ">=" -> tVal.compareTo(uVal) >= 0;
                    default -> false;
                };

                if (match) rows.add(concat(t,u));
            }
        }        
                
        return new Table (name + count++, concat (attribute, table2.attribute),
                                          concat (domain, table2.domain), key, rows);
    } // join

    /************************************************************************************
     * Join this table and table2 by performing an NATURAL JOIN.  Tuples from both tables
     * are compared requiring common attributes to be equal.  The duplicate column is also
     * eliminated.
     *
     * #usage deposit.join (customer)
     *
     * @param table2  the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (Table table2)
    {
        // Find common attributes
        List <String> commonAttrs = new ArrayList <> ();
        for (int i = 0; i < attribute.length; i++) {
            for (int j = 0; j < table2.attribute.length; j++) {
                if (attribute[i].equals (table2.attribute[j])) {
                    commonAttrs.add (attribute[i]);
                    break;
                } // if
            } // for
        } // for

        // Build new attribute list (all from table 1 + non-common from table2)
        List <String> newAttrs = new ArrayList <> ();
        for (String attr : attribute) newAttrs.add (attr);
        for (String attr : table2.attribute) {
            if (!commonAttrs.contains (attr)) {
                newAttrs.add (attr);
            } // if
        } // for

        // Build new domain list
        Class [] newDom = new Class [newAttrs.size ()];
        for (int i = 0; i < attribute.length; i++) {
            newDom[i] = domain[i];
        } // for
        int idx = attribute.length;
        for (String attr : table2.attribute) {
            if (!commonAttrs.contains (attr)) {
                int col2 = table2.col.get (attr);
                newDom[idx++] = table2.domain[col2];
            } // if
        } // for

        // Match and combine tuples
        List <Comparable []> rows = new ArrayList <> ();
        for (var t : tuples) {
            for (var u : table2.tuples) {
                // Check if common attributes match
                boolean match = true;
                for (var commonAttr : commonAttrs) {
                    int colT = col.get (commonAttr);
                    int colU = table2.col.get (commonAttr);
                    if (!t[colT].equals (u[colU])) {
                        match = false;
                        break;
                    } // if
                } // for

                if (match) {
                    // Combine tuples
                    Comparable [] combined = new Comparable [newAttrs.size ()];
                    // Copy all from t
                    for (int i = 0; i < t.length; i++) {
                        combined[i] = t[i];
                    } // for
                    // Copy non-common from u
                    int destIdx = t.length;
                    for (var attr : table2.attribute) {
                        if (!commonAttrs.contains (attr)) {
                            int colU = table2.col.get (attr);
                            combined[destIdx++] = u[colU];
                        } // if
                    } // for
                    rows.add (combined);
                } // if
            } // for
        } // for

        return new Table (name + count++, newAttrs.toArray (new String [0]), newDom, key, rows);
    } // join

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join" equating a FOREIGN KEY in
     * the first table (this) with the PRIMARY KEY in the second table (table2).
     * Use the INDEXED JOIN algorithm for efficient execution.
     *
     * #usage deposit.join (customer, "cname")
     *
     * @param table2  the rhs table in the join operation
     * @param fkey    the attributes of this table to be compared (Foreign Key)
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (Table table2, String fkey)
    {
        var rows = new ArrayList <Comparable []> ();
        String [] fkeyAttrs = fkey.split (" ");

        for (Comparable [] t : tuples) {
            Comparable [] fkeyVal = new Comparable [table2.key.length];

            for (int i = 0; i < table2.key.length; i++) {
                fkeyVal [i] = t [col.get(fkeyAttrs[i])];
            }

            Comparable [] match = table2.index.get(new KeyType (fkeyVal));

            if (match != null) {
                rows.add (concat (t, match));
            }
        }

        return new Table (name + count++, concat (attribute, table2.attribute),
                                          concat (domain, table2.domain), key, rows);
    } // join

    /************************************************************************************
     * Concatenate two arrays of type T to form a new wider array.
     *
     * @see http://stackoverflow.com/questions/80476/how-to-concatenate-two-arrays-in-java
     *
     * @param arr1  the first array
     * @param arr2  the second array
     * @return  a wider array containing all the values from arr1 and arr2
     */
    private static <T> T [] concat (T [] arr1, T [] arr2)
    {
        T [] result = Arrays.copyOf (arr1, arr1.length + arr2.length);
        arraycopy (arr2, 0, result, arr1.length, arr2.length);
        return result;
    } // concat

    /************************************************************************************
     * Print the given tuple.
     * @param tup  the tuple to print.
     */
    public void printTup (Comparable [] tup)
    {
        out.print ("| ");
        for (var attr : tup) out.printf ("%15s", attr);
        out.println (" |");
    } // printTup
    
    /************************************************************************************
     * Show/print this table.
     */
    public void show ()
    {
        out.println ("\n Table " + name);
        out.print ("|-" + "---------------".repeat (attribute.length));
        out.println ("-|");
        out.print ("| ");
        for (var a : attribute) out.printf ("%15s", a);
        out.println (" |");
        out.print ("|-" + "---------------".repeat (attribute.length));
        out.println ("-|");
        for (var tup : tuples) printTup (tup);
        out.print ("|-" + "---------------".repeat (attribute.length));
        out.println ("-|");
    } // show

    /************************************************************************************
     * Compare two tuples to see if they match (all columns must match).
     *
     * @param t1  first tuple
     * @param t2  second tuple
     * @return  true if all columns are equal, false otherwise
     */
    private boolean tupleCompare (Comparable [] t1, Comparable [] t2)
    {
        if (t1.length != t2.length) return false;
        for (int i = 0; i < t1.length; i++) {
            if (!t1[i].equals (t2[i])) return false;
        } // for
        return true;
    } // tupleCompare

} // Table

