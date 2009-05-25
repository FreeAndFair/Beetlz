package input;

import ie.ucd.bon.ast.BONType;
import ie.ucd.bon.ast.IndexClause;
import ie.ucd.bon.ast.Indexing;
import ie.ucd.bon.ast.Type;
import ie.ucd.bon.typechecker.ClassDefinition;
import ie.ucd.bon.typechecker.ClusterDefinition;
import ie.ucd.bon.typechecker.FeatureArgument;
import ie.ucd.bon.typechecker.FeatureSpecification;
import ie.ucd.bon.typechecker.FeatureSpecificationInstance;
import ie.ucd.bon.typechecker.FormalGeneric;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;


import structure.ClassStructure;
import structure.FeatureStructure;
import structure.Invariant;
import structure.Signature;
import structure.Spec;
import structure.Visibility;
import utils.BConst;
import utils.FeatureType;
import utils.SourceLocation;
import utils.ModifierManager.ClassModifier;
import utils.ModifierManager.ClassType;
import utils.ModifierManager.FeatureModifier;
import utils.ModifierManager.VisibilityModifier;
import utils.smart.FeatureSmartString;
import utils.smart.GenericParameter;
import utils.smart.ParametrizedSmartString;
import utils.smart.SmartString;
import utils.smart.TypeSmartString;
import utils.smart.WildcardSmartString;
import log.CCLevel;
import log.CCLogRecord;
import logic.Expression.ArithmeticExpression;
import logic.Expression.EqualityExpression;
import logic.Expression.EquivalenceExpression;
import logic.Expression;
import logic.Expression.ImpliesExpression;
import logic.Expression.InformalExpression;
import logic.Expression.Keyword;
import logic.Expression.LiteralExpression;
import logic.Expression.IdentifierExpression;
import logic.Expression.LogicalExpression;
import logic.Expression.MemberaccessExpression;
import logic.Expression.MethodcallExpression;
import logic.Expression.Nullity;
import logic.Operator;
import logic.Expression.RelationalExpression;
import logic.Expression.UnaryExpression;
import logic.Expression.Keyword.Keywords;
import main.Beetlz;

/**
 * Parser for BON information from BONc data structures.
 * Takes BONc elements and puts the
 * required information into our own data structures.
 * @author Eva Darulova (edarulova@googlemail.com)
 * @version beta-1
 */
public final class BONParser {
  /** */
  private BONParser() { }
  /**
   * Parse classes.
   * @param a_class class definition
   * @param a_cluster c cluster the class belongs to
   * @return parsed class
   */
  public static ClassStructure parseClass(final ClassDefinition a_class,
                                          final Set < ClusterDefinition > a_cluster) {
    final int two = 2;
    final SortedSet  <  ClassModifier  > mod    = new TreeSet < ClassModifier > ();
    final Visibility vis                        = new Visibility(VisibilityModifier.PUBLIC);
    final List  < SmartString > generics   = new Vector < SmartString > ();
    final SortedSet < SmartString > interfaces  = new TreeSet < SmartString > ();
    Invariant inv                               = null;
    final List < SmartString > clus             = new Vector < SmartString > ();
    final List < String > about = new Vector < String > ();
    String author = ""; //$NON-NLS-1$
    String version = ""; //$NON-NLS-1$
    String all_else = ""; //$NON-NLS-1$
    //Comments
    if (a_class.getIndexing() != null) {
      final Indexing index = a_class.getIndexing();
      for (final IndexClause i : index.getIndexes()) {
        if (i.getId().equals("about")) { //$NON-NLS-1$
          for (final String s : i.getIndexTerms()) {
            about.add(s.replace("\"", ""));  //$NON-NLS-1$//$NON-NLS-2$
          }
        } else if (i.getId().equals("author")) { //$NON-NLS-1$
          String a = ""; //$NON-NLS-1$
          for (final String s : i.getIndexTerms()) {
            a += s.replace("\"", "") + ", ";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
          }
          if (a.length() > two) {
            a = a.substring(0, a.length() - two);
          }
          author += a;
        } else if (i.getId().equals("version")) { //$NON-NLS-1$
          String v = ""; //$NON-NLS-1$
          for (final String s : i.getIndexTerms()) {
            v += s.replace("\"", "") + ", ";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
          }
          if (v.length() > two) {
            v = v.substring(0, v.length() - two);
          }
          version += v;
        } else {
          String a = ""; //$NON-NLS-1$
          for (final String s : i.getIndexTerms()) {
            a += s.replace("\"", "" + ",");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
          }
          if (a.length() > two) {
            a = a.substring(0, a.length() - two);
          }
          all_else += a;
        }
      }
    }
    //Modifier
    if (a_class.isDeferred()) {
      mod.add(ClassModifier.ABSTRACT);
    }
    if (a_class.isEffective()) {
      mod.add(ClassModifier.EFFECTIVE);
    }
    if (a_class.isRoot()) {
      mod.add(ClassModifier.ROOT);
    }
    if (a_class.isInterfaced()) {
      mod.add(ClassModifier.INTERFACED);
    }
    if (a_class.isPersistent()) {
      mod.add(ClassModifier.PERSISTENT);
    }
    if (a_class.isReused()) {
      mod.add(ClassModifier.REUSED);
    }
    //Generics
    if (a_class.hasFormalGenerics()) {
      mod.add(ClassModifier.GENERIC);
      final Collection < FormalGeneric > g = a_class.getFormalGenerics();
      if (g.size()  > 0) {
        for (final FormalGeneric f : g) {
          generics.add(getFormalGeneric(f));
        }
      }
    }
    //Class name
    final TypeSmartString name = new TypeSmartString(a_class.getName());

    //Inheritance
    final Set < BONType > superC = a_class.getParentClasses();

    if (superC != null) {
      for (final BONType s : superC) {
        final boolean success = interfaces.add(getType(s));
        if (!success) {
          Beetlz.getWaitingRecords().
            add(new CCLogRecord(CCLevel.JAVA_WARNING, null,
                              String.format(Beetlz.getResourceBundle().
                                            getString("BONParser." + //$NON-NLS-1$
                                            "repeatedInheritanceNotSupported"), //$NON-NLS-1$
                                            s, name)));
        }
      }
    }
    //Invariant
    inv = parseInvariant(a_class.getInvariants());
    //Cluster
    if (a_cluster != null) {
      final Iterator < ClusterDefinition > iter = a_cluster.iterator();
      while (iter.hasNext()) {
        clus.add(new SmartString(iter.next().getName()));
      }
    }
    //Source location
    final SourceLocation src = new SourceLocation(a_class.getSourceLocation()
                                                  .getSourceFile(),
                                                  a_class.getSourceLocation().
                                                  getLineNumber());
    //Create class
    final ClassStructure parsedClass =
      new ClassStructure(ClassType.BON, mod, vis, generics, name,
                         interfaces, clus, src);
    parsedClass.setComment(about, author, version, all_else);
    parsedClass.setInvariant(inv);
    //Get the features
    for (final FeatureSpecificationInstance feat : a_class.getFeatures()) {
      final FeatureStructure f = parseFeature(feat, parsedClass);
      if (!Beetlz.getProfile().pureBon()) {
        if (f.getSimpleName().equals(BConst.MAKE) ||
            f.getSimpleName().matches(BConst.MAKE + "[0-9]")) { //$NON-NLS-1$
          parsedClass.addConstructor(f);
          continue;
        }
      }
      parsedClass.addFeature(f);
    }
    return parsedClass;
  }

  /**
   * Parse a feature.
   * @param a_feature feature to parse
   * @param a_encl_class enclosing class
   * @return parsed feature
   */
  private static FeatureStructure parseFeature(final FeatureSpecificationInstance a_feature,
                                               final ClassStructure a_encl_class) {
    //full feature
    final SortedSet < FeatureModifier > mod = new TreeSet < FeatureModifier > ();
    Visibility vis                          = new Visibility(VisibilityModifier.PUBLIC);
    final FeatureSpecification fSpec        = a_feature.getFeatureSpec();
    String rename_class                      = null;
    String rename_feature                    = null;
    //Modifier
    if (fSpec.isDeferred()) {
      mod.add(FeatureModifier.ABSTRACT);
    }
    if (fSpec.isRedefined()) {
      mod.add(FeatureModifier.REDEFINED);
    }
    if (fSpec.isEffective()) {
      mod.add(FeatureModifier.EFFECTIVE);
    }
    //Visibility
    if (fSpec.getFeature().isPrivate()) {
      vis = new Visibility(VisibilityModifier.PRIVATE);
    } else if (fSpec.getFeature().getExports().size() > 0) {
      final Set < String > exp = fSpec.getFeature().getExports();
      if (exp.size() == 1 && exp.contains(a_encl_class.getSimpleName())) {
        vis = new Visibility(VisibilityModifier.PROTECTED);
      } else if (exp.size() == 1 && exp.contains(a_encl_class.getInnermostPackage().
                                                 toString())) {
        vis = new Visibility(VisibilityModifier.PACKAGE_PRIVATE);
      } else {
        vis = new Visibility(VisibilityModifier.RESTRICTED);
      }
      final List < TypeSmartString > exports = new Vector < TypeSmartString > ();
      for (final String s : fSpec.getFeature().getExports()) {
        exports.add(new TypeSmartString(s));
      }
      vis.setExports(exports);
    }
    //Feature name
    final FeatureSmartString name = new FeatureSmartString(a_feature.getName());
    //Return type
    SmartString return_value = SmartString.getVoid(); //default is void
    final Map < String , SmartString > params = new HashMap < String, SmartString > ();
    if (fSpec.getType() != null) {
      return_value = getType(fSpec.getType());
    }
    //Parameter
    for (final FeatureArgument a : fSpec.getArgs().values()) {
      params.put(a.getName(), getType(a.getType()));
    }
    //Specs?
    final Spec spec = BONParser.parseFeatureSpecs(fSpec.getPrecondition(),
                                                  fSpec.getPostcondition(),
                                                  return_value, name.toString(), params);
    final List < Spec > specCases = new Vector < Spec > ();
    specCases.add(spec);
    final Signature sign = Signature.getBonSignature(return_value, params);
    //SourceLocation
    final SourceLocation src =
      new SourceLocation(a_feature.getSourceLocation().getSourceFile(),
                         a_feature.getSourceLocation().getLineNumber());
    //Renaming
    if (a_feature.getFeatureSpec().getRenaming() != null) {
      rename_class = a_feature.getFeatureSpec().getRenaming().getClassName();
      rename_feature = a_feature.getFeatureSpec().getRenaming().getFeatureName();
    }
    //Client relations
    if (fSpec.getTypeMark() != null && fSpec.getTypeMark().isSharedMark()) {
      a_encl_class.addSharedAssociation(return_value);
    }
    if (fSpec.getTypeMark() != null && fSpec.getTypeMark().isAggregate()) {
      a_encl_class.addAggregation(return_value);
    }
    return new FeatureStructure(mod, vis, name, sign,
                                specCases, src, rename_class,
                                rename_feature, a_encl_class);
  }

  /**
   * Parse an invariant.
   * @param the_invariants invariant clauses to parse.
   * @return parsed invariant
   */
  private static Invariant parseInvariant(final Collection < String > the_invariants) {
    final List < Expression > clauses = new Vector < Expression > ();
    final List < Expression > history = new Vector < Expression > ();

    for (final String invar : the_invariants) {
      final String[] parts = invar.split(";"); //$NON-NLS-1$
      for (final String s : parts) {
        final Expression e = parseExpression(s.trim());
        clauses.addAll(splitBooleanExpressions(e));
      }
    }
    return new Invariant(clauses, history);
  }

  /**
   * These operators are being recognized.
   * 1:  [ ]  (array index)  ()method call .member access
   * 2:  + -    unary plus, minus
   *     not    boolean (logical) NOT
   * 3:  * / % //(integer division) binary
   * 4:  + -   binary
   *     +    string concatenation (dunno)
   * 5:  <, <=, >, >=
   * 6:   ==,  !=
   * 7: &&  boolean (logical) AND
   * 8: ||  boolean (logical) OR
   * 9: -> implication
   * 10: <-> not<->  equivalent
   * @param a_string expression to parse
   * @return parsed expression
   */
  private static Expression parseExpression(final String a_string) {
    final int two = 2;
    //We need to go bottom up through the precedence hierarchy:
    final String s = a_string.trim();
    if (s.startsWith("--")) { //$NON-NLS-1$
      return new InformalExpression(s.substring(two));
    }
    if (s.startsWith("(") && s.endsWith(")")) { //$NON-NLS-1$ //$NON-NLS-2$
      final Expression e = parseExpression(s.substring(1, s.length() - 1));
      e.setParenthesised();
      return e;
    }
    if (s.contains("<->")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, "<->"); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.IFF);
    }
    if (s.contains("->")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, "->"); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.IMPLIES);
    }
    if (s.contains(" or ")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, " or "); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.OR);
    }
    if (s.contains(" and ")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, " and "); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.AND);
    }
    if (s.contains("xor")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, " xor "); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.XOR);
    }
    if (s.contains(" /= ")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, "/="); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.NOT_EQUAL);
    }
    if (s.contains(" = ")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, "="); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.EQUAL);
    }

    if (s.contains(" <= ")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, "<="); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.SMALLER_EQUAL);
    } else if (s.contains(">=")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, ">="); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.GREATER_EQUAL);
    } else if (s.contains("<")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, "<"); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.SMALLER);
    } else if (s.contains(">")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, ">"); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.GREATER);
    }

    if (s.contains("-")) { //$NON-NLS-1$
      if (s.indexOf("-") > 0) { //$NON-NLS-1$
        final String[] parts = mySplit(s, "-"); //$NON-NLS-1$
        if (parts.length != 0)  return getTwoPartExpression(parts, Operator.MINUS);
      }
      //else continue, since we have an unary minus.. probably

    }
    if (s.contains("+")) { //$NON-NLS-1$
      if (s.indexOf("+") > 0) { //$NON-NLS-1$
        final String[] parts = mySplit(s, "+"); //$NON-NLS-1$
        if (parts.length != 0)  return getTwoPartExpression(parts, Operator.PLUS);
      }
    }
    if (s.contains("\\\\")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, "\\\\"); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.MODULO);
    }
    if (s.contains("/")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, "/"); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.DIVIDE);
    }
    if (s.contains("//")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, "//"); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.DIVIDE);
    }
    if (s.contains("*")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, "*"); //$NON-NLS-1$
      if (parts != null)  return getTwoPartExpression(parts, Operator.MULTIPLE);
    }
    if (s.contains("^")) { //$NON-NLS-1$
      final String[] parts = mySplit(s, "^"); //$NON-NLS-1$
      if (parts.length != 0)  return getTwoPartExpression(parts, Operator.POWER);
    }
    if (s.startsWith("not ")) { //$NON-NLS-1$
      final String expr = s.replaceFirst("not", ""); //$NON-NLS-1$ //$NON-NLS-2$
      return new UnaryExpression(Operator.NOT, parseExpression(expr));

    }
    if (s.contains("+")) { //should be unary now //$NON-NLS-1$
      final String expr = s.replaceFirst("\\+", ""); //$NON-NLS-1$ //$NON-NLS-2$
      return new UnaryExpression(Operator.UNARY_PLUS, parseExpression(expr));
    }
    if (s.contains("-")) { //should be unary now //$NON-NLS-1$
      final String expr = s.replaceFirst("-", ""); //$NON-NLS-1$ //$NON-NLS-2$
      return new UnaryExpression(Operator.UNARY_MINUS, parseExpression(expr));
    }

    if (s.contains("(") && s.contains(")")) { //$NON-NLS-1$ //$NON-NLS-2$
      return getMethodcall(s);
    }
    if (s.contains(".")) { //$NON-NLS-1$
      return getMemberaccess(s);
    }
    if (s.startsWith("old ")) { //$NON-NLS-1$
      final String expr = s.replaceFirst("old", ""); //$NON-NLS-1$ //$NON-NLS-2$
      final List < Expression > list = new Vector < Expression > ();
      list.add(parseExpression(expr));
      return new MethodcallExpression(new IdentifierExpression("old"), list); //$NON-NLS-1$
    }

    //keywords!
    if (s.equals("Result")) { //$NON-NLS-1$
      return new Keyword(Keywords.RESULT);
    }
    if (s.equals("Void")) { //$NON-NLS-1$
      return new Keyword(Keywords.VOID);
    }
    if (s.equals("Current")) { //$NON-NLS-1$
      return new Keyword(Keywords.CURRENT);
    }
    if (s.equals("true")) { //$NON-NLS-1$
      return new Keyword(Keywords.TRUE);
    }
    if (s.equals("false")) { //$NON-NLS-1$
      return new Keyword(Keywords.FALSE);
    }

    if (s.startsWith("\"") && s.endsWith("\"")) { //$NON-NLS-1$ //$NON-NLS-2$
      return new LiteralExpression(s);
    }
    if (s.startsWith("'") && s.endsWith("'")) { //$NON-NLS-1$ //$NON-NLS-2$
      return new LiteralExpression(s);
    }
    if (s.matches("\\d+")) { //only numbers, hopefully //$NON-NLS-1$
      return new LiteralExpression(s);
    }
    if (!s.contains(" ")) { //$NON-NLS-1$
      return new IdentifierExpression(s);
    } else {
      return new InformalExpression(s);
    }

  }

  /**
   * Splits a string by an operator.
   * @param an_expr expression to split
   * @param an_op operator to split the expression on
   * @return split expression
   */
  private static String[] mySplit(final String an_expr, final String an_op) {
    final int index = an_expr.indexOf(an_op);
    if (index == -1) {
      return new String[0];
    }

    if (index < an_expr.length()) {
      final String first = an_expr.substring(0, index);
      final String second = an_expr.substring(index + an_op.length());
      if (numberOfChars('(', first) == numberOfChars(')', first) &&
          numberOfChars('(', second) == numberOfChars(')', second)) {
        return new String[] {first, second};
      }
    }
    return new String[0]; //failed to split
  }

  /**
   * Get number of occurences of a certain character.
   * @param a_character character to check
   * @param a_string string to search
   * @return count of chars
   */
  private static int numberOfChars(final char a_character,
                                   final String a_string) {
    int i = 0;
    for (final char c : a_string.toCharArray()) {
      if (a_character == c) i++;
    }
    return i;
  }

  /**
   * Create a Expression.
   * @param the_parts parts to create an expression from
   * @param the_operator operator
   * @return parsed expression
   */
  private static Expression getTwoPartExpression(final String[] the_parts,
                                                 final Operator the_operator) {
    final int two = 2;
    if (the_parts.length == two) {
      switch (the_operator) {
        case MULTIPLE: case DIVIDE: case MODULO: case INT_DIVIDE: case PLUS:
        case MINUS: case POWER:
          return new ArithmeticExpression(parseExpression(the_parts[0]),
                                          the_operator, parseExpression(the_parts[1]));
        case GREATER: case SMALLER: case GREATER_EQUAL: case SMALLER_EQUAL:
          return new RelationalExpression(parseExpression(the_parts[0]),
                                          the_operator, parseExpression(the_parts[1]));
        case EQUAL: case NOT_EQUAL:
          return new EqualityExpression(parseExpression(the_parts[0]),
                                        the_operator, parseExpression(the_parts[1]));
        case AND: case OR: case XOR:
          return new LogicalExpression(parseExpression(the_parts[0]),
                                       the_operator, parseExpression(the_parts[1]));
        case IMPLIES:
          return new ImpliesExpression(parseExpression(the_parts[0]),
                                       parseExpression(the_parts[1]));
        case IFF: case NOT_IFF:
          return new EquivalenceExpression(parseExpression(the_parts[0]),
                                           the_operator, parseExpression(the_parts[1]));
        default:
          return new InformalExpression(the_parts[0].trim());
      }
    }
    return new InformalExpression(the_parts[0].trim());
  }

  /**
   * Parse a method call.
   * @param a_str string to parse
   * @return parsed expression
   */
  private static Expression getMethodcall(final String a_str) {
    final int ilast = a_str.lastIndexOf(")"); //$NON-NLS-1$
    final int ifirst = a_str.indexOf("("); //$NON-NLS-1$
    final String methodname = a_str.substring(0, ifirst);
    final String args = a_str.substring(ifirst + 1, ilast);

    final List < Expression > list = new Vector < Expression > ();
    final String[] parts = args.split(","); //$NON-NLS-1$
    for (final String a : parts) {
      list.add(parseExpression(a));
    }

    return new MethodcallExpression(parseExpression(methodname), list);
  }

  /**
   * Parse member access.
   * @param a_str string to parse
   * @return parsed expression
   */
  private static Expression getMemberaccess(final String a_str) {
    final int dot = a_str.lastIndexOf("."); //$NON-NLS-1$
    final String source = a_str.substring(0, dot);
    final String field = a_str.substring(dot + 1);
    return new MemberaccessExpression(parseExpression(source),
                                      parseExpression(field));
  }

  /**
   * Parse feature specs.
   * @param a_pre precondition
   * @param a_post postcondition
   * @param a_return_value return value
   * @param a_feature feature name
   * @param the_params parameter
   * @return specs
   */
  public static Spec parseFeatureSpecs(final String a_pre, final String a_post,
                                       final SmartString a_return_value,
                                       final String a_feature,
                                       final Map < String , SmartString > the_params) {
    final List < Expression > preconditions = new Vector < Expression > ();
    final List < Expression > postconditions = new Vector < Expression > ();
    SortedSet < FeatureSmartString > frame = new TreeSet < FeatureSmartString > ();
    FeatureType type;
    String constant = null;

    if (a_pre != null) {
      final String[] parts = a_pre.split(";"); //$NON-NLS-1$
      for (final String s : parts) {
        if (s != null) {
          final boolean nul = parseArgumentNullity(s, the_params);
          if (!nul) {
            final Expression e = parseExpression(s.trim());
            preconditions.addAll(splitBooleanExpressions(e));
          }
        }
      }
    }

    if (a_post != null) {
      final String[] parts = a_post.split(";"); //$NON-NLS-1$
      for (final String s : parts) {
        if (s != null) {
          if (s.contains("delta")) { //$NON-NLS-1$
            frame = parseFrameConstraint(s);
          } else {
            final Nullity nul = parseReturnNullity(s.trim());
            if (nul != null) {
              a_return_value.setNullity(nul);
            } else {
              constant = parseConstant(s, a_feature);
              if (constant == null) {
                final Expression e = parseExpression(s.trim());
                postconditions.addAll(splitBooleanExpressions(e));
              }
            }
          }
        }
      }
    }

    //Query or Command ?!
    if (a_return_value.equals(SmartString.getVoid())) {
      type = FeatureType.COMMAND;
      if (frame.size() == 0) {
        frame.add(FeatureSmartString.everything());
      }
    } else if (frame.size() == 0) {
      type = FeatureType.QUERY;
      frame.add(FeatureSmartString.nothing());
    } else {
      type = FeatureType.MIXED;
    }
    return new Spec(preconditions, postconditions, frame,
                    constant, type, ClassType.BON);
  }

  /**
   * Parse a frame constraint.
   * @param a_frame frame to parse
   * @return frame
   */
  private static SortedSet < FeatureSmartString > parseFrameConstraint(final String a_frame) {
    final SortedSet < FeatureSmartString > frame = new TreeSet < FeatureSmartString > ();
    final String[] parts =
      a_frame.replace("delta", "").split(",");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
    for (final String s : parts) {
      frame.add(new FeatureSmartString(s.
                                       replace("}", "").//$NON-NLS-1$   //$NON-NLS-2$
                                       replace("{", "").trim())); //$NON-NLS-1$   //$NON-NLS-2$
    }
    return frame;
  }

  /**
   * Parse argument nullity.
   * @param a_str string
   * @param the_params parameter
   * @return true if nullity parsed
   */
  private static boolean parseArgumentNullity(final String a_str,
                                              final Map < String,
                                              SmartString > the_params) {
    for (final String p : the_params.keySet()) {
      if (a_str.matches(p + "\\s*/=\\s*Void")) { //$NON-NLS-1$
        the_params.get(p).setNullity(Nullity.NON_NULL);
        return true;
      }
    }
    return false;
  }

  /**
   * Parse return type nullity.
   * @param a_str string
   * @return nullity
   */
  private static Nullity parseReturnNullity (final String a_str) {
    if (a_str.matches("Result\\s*/=\\s*Void")) { //$NON-NLS-1$
      return Nullity.NON_NULL;
    }
    return null;
  }

  /**
   * Parse constant value.
   * @param a_str potential constant expression
   * @param a_feature_name feature name
   * @return constant, if the expression specifies one
   */
  private static String parseConstant(final String a_str, final String a_feature_name) {
    if (a_str.matches("Result\\s*=\\s*old\\s*" + a_feature_name)) { //$NON-NLS-1$
      return Spec.UNKNOWN_VALUE;
    }
    if (a_str.matches("Result\\s*=.*")) { //$NON-NLS-1$
      String type = a_str.replace("Result", "");  //$NON-NLS-1$//$NON-NLS-2$
      type = type.replace("=", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
      if ((type.startsWith("\"") && type.endsWith("\"")) || //$NON-NLS-1$ //$NON-NLS-2$
          (type.startsWith("'") && type.endsWith("'")) || //$NON-NLS-1$ //$NON-NLS-2$
          type.matches("\\d+")) { //$NON-NLS-1$
        return type;
      }
    }
    return null;
  }

  /**
   * Parse type.
   * @param a_type type to parse
   * @return parsed type
   */
  private static SmartString getType(final BONType a_type) {

    if (a_type.getActualGenerics() == null) {
      return new TypeSmartString(a_type.getIdentifier());
    }
    final SmartString name = new TypeSmartString(a_type.getIdentifier());
    final List < SmartString > params = new Vector < SmartString > ();
    for (final BONType t : a_type.getActualGenerics()) {
      if (t.getFullString().equals("ANY")) { //$NON-NLS-1$
        params.add(WildcardSmartString.getBONWildcard());
      } else {
        params.add(getType(t));
      }
    }
    return new ParametrizedSmartString(a_type.getFullString(), name, params);
  }

  /**
   * Parse type.
   * @param a_type type to parse
   * @return parsed type
   */
  private static SmartString getType(final Type a_type) {
    if (a_type.getActualGenerics() == null) {
      return new TypeSmartString(a_type.getIdentifier());
    }
    final SmartString name = new TypeSmartString(a_type.getIdentifier());
    final List < SmartString > params = new Vector < SmartString > ();

    for (final BONType t : a_type.getActualGenerics()) {
      if (t.getFullString().equals("ANY")) { //$NON-NLS-1$
        params.add(WildcardSmartString.getBONWildcard());
      } else {
        params.add(getType(t));
      }
    }
    return new ParametrizedSmartString(a_type.getFullString(), name, params);

  }

  /**
   * Parse generic parameters.
   * @param a_generic formal params
   * @return parsed generic parameter
   */
  private static SmartString getFormalGeneric(final FormalGeneric a_generic) {
    if (a_generic.getType() != null) {
      final List < SmartString > list = new Vector < SmartString > ();
      list.add(getType(a_generic.getType()));
      //System.err.println(form.getName() + "  " + form.getType().toString());
      final String name = a_generic.getName() + " -> " +
        a_generic.getType().getFullString(); //$NON-NLS-1$
      return new GenericParameter(name, a_generic.getName(), list);
    }
    return new GenericParameter(a_generic.getName(), a_generic.getName(), null);
  }

  /**
   * Split a boolean expression based on AND.
   * @param an_expr expression to split
   * @return list of split expressions
   */
  private static List < Expression > splitBooleanExpressions(final Expression an_expr) {
    final List < Expression > list = new Vector < Expression > ();
    if (an_expr instanceof LogicalExpression) {
      final LogicalExpression and = (LogicalExpression) an_expr;
      if (and.getOperator() == Operator.AND) {
        list.addAll(splitBooleanExpressions(and.leftExpression()));
        list.addAll(splitBooleanExpressions(and.rightExpression()));
        return list;
      }
    }
    list.add(an_expr);
    return list;
  }
}