package fr.lip6.mg4j.extensions.trec;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.parser.Attribute;
import it.unimi.dsi.parser.Element;
import it.unimi.dsi.parser.Entity;
import it.unimi.dsi.parser.ParsingFactory;

/**
 * A parsing factory for TREC (AP/WSJ collections) documents
 */
public class TRECParsingFactory implements ParsingFactory {

	@Override
	public Element getElement(final MutableString name) {
		return NAME2ELEMENT.get(name.toString().toLowerCase());
	}

	@Override
	public Attribute getAttribute(final MutableString name) {
		return NAME2ATTRIBUTE.get(name.toString().toLowerCase());
	}

	@Override
	public Entity getEntity(final MutableString name) {
		return NAME2ENTITY.get(name.toString());
	}

	public static final TRECParsingFactory INSTANCE = new TRECParsingFactory();

	/** A (quick) map from entity names to entites. */
	static final Object2ObjectOpenHashMap<String, Entity> NAME2ENTITY = new Object2ObjectOpenHashMap<String, Entity>(
			Hash.DEFAULT_INITIAL_SIZE, .5f);

	/** A (quick) map from attribute names to attributes. */
	static final Object2ObjectOpenHashMap<String, Attribute> NAME2ATTRIBUTE = new Object2ObjectOpenHashMap<String, Attribute>(
			Hash.DEFAULT_INITIAL_SIZE, .5f);

	/** A (quick) map from element-type names to element types. */
	static final Object2ObjectOpenHashMap<String, Element> NAME2ELEMENT = new Object2ObjectOpenHashMap<String, Element>(
			Hash.DEFAULT_INITIAL_SIZE, .5f);

	static Element newElement(final String name) {
		final Element element = new Element(name);
		NAME2ELEMENT.put(element.name.toString(), element);
		return element;
	}

	static Element newElement(final String name, boolean breaksFlow,
			boolean isSimple) {
		final Element element = new Element(name, breaksFlow, isSimple);
		NAME2ELEMENT.put(element.name.toString(), element);
		return element;
	}

	static Entity newEntity(final String name, final char c) {
		final Entity entity = new Entity(name, c);
		NAME2ENTITY.put(entity.name.toString(), entity);
		return entity;
	}

	static Attribute newAttribute(final String name) {
		final Attribute attribute = new Attribute(name);
		NAME2ATTRIBUTE.put(attribute.name.toString(), attribute);
		return attribute;
	}

	/*
	 * Attributes
	 */
	public static final Attribute ATTR_NUMBER = newAttribute("number");
	public static final Attribute ATTR_TYPE = newAttribute("type");

	/*
	 * Elements
	 */
	public static final Element ELEMENT_NUM = newElement("num"),
			ELEMENT_TOP = newElement("top"),
			ELEMENT_TITLE = newElement("title"),
			ELEMENT_DESC = newElement("desc"),
			
			// Summary  
			ELEMENT_NARR = newElement("narr", true, true),
			ELEMENT_SMRY = newElement("smry"),
			
			// Definition in trec topic
			ELEMENT_DEF = newElement("def"),
			
			// Concept in trec topic
			ELEMENT_CON = newElement("con"),

			// javascript and friends
			ELEMENT_SCRIPT = newElement("script", true, true),
			
			
			ELEMENT_BODY = newElement("body", true, true),
			ELEMENT_TEXT = newElement("text", true, true),
			ELEMENT_P = newElement("p", true, true),
			ELEMENT_SECTION = newElement("section", true, true),
			ELEMENT_LEADPARA = newElement("leadpara", true, true),
			ELEMENT_DESCRIPT = newElement("descript", true, true),
			ELEMENT_FIRST = newElement("first", true, true),
			ELEMENT_SECOND = newElement("second", true, true),
			ELEMENT_PAGE = newElement("page", true, true),
			ELEMENT_MEMO = newElement("memo", true, true),
			ELEMENT_PUB = newElement("pub", true, true),
			ELEMENT_SLUG = newElement("slug", true, true),
			ELEMENT_HEADLINE = newElement("headline", true, true),
			ELEMENT_CENTER = newElement("center", true, true),
			ELEMENT_TTL = newElement("ttl", true, true),
			ELEMENT_DATE = newElement("date", true, true),
			ELEMENT_DATETIME = newElement("date_time", true, true),
			ELEMENT_PUBDATE = newElement("pubdate", true, true),
			ELEMENT_DOCNO = newElement("docno", true, true),
			ELEMENT_DOCID = newElement("docid", true, true),
			ELEMENT_DOCHDR = newElement("dochdr", true, true),	
			ELEMENT_HEADER = newElement("header", true, true),
			ELEMENT_PROFILE = newElement("profile", true, true),
			ELEMENT_COPYRGHT = newElement("copyrght", true, true),
			ELEMENT_FILEID = newElement("fileid", true, true),
			ELEMENT_BYLINE = newElement("byline", true, true),

			// Elements used for contemporary Web track topics
			ELEMENT_TOPIC = newElement("topic"),
			ELEMENT_QUERY = newElement("query"),
			ELEMENT_DESCRIPTION = newElement("description"),
			ELEMENT_SUBTOPIC = newElement("subtopic");
	

	static {
		NAME2ATTRIBUTE.defaultReturnValue(Attribute.UNKNOWN);
		NAME2ELEMENT.defaultReturnValue(Element.UNKNOWN);

		// --- Entity Names -----------------------------------

		// Latin 1
		newEntity("nbsp", (char) 160);
		newEntity("iexcl", (char) 161);
		newEntity("cent", (char) 162);
		newEntity("pound", (char) 163);
		newEntity("curren", (char) 164);
		newEntity("yen", (char) 165);
		newEntity("brvbar", (char) 166);
		newEntity("sect", (char) 167);
		newEntity("uml", (char) 168);
		newEntity("copy", (char) 169);
		newEntity("ordf", (char) 170);
		newEntity("laquo", (char) 171);
		newEntity("not", (char) 172);
		newEntity("shy", (char) 173);
		newEntity("reg", (char) 174);
		newEntity("macr", (char) 175);
		newEntity("deg", (char) 176);
		newEntity("plusmn", (char) 177);
		newEntity("sup2", (char) 178);
		newEntity("sup3", (char) 179);
		newEntity("acute", (char) 180);
		newEntity("micro", (char) 181);
		newEntity("para", (char) 182);
		newEntity("middot", (char) 183);
		newEntity("cedil", (char) 184);
		newEntity("sup1", (char) 185);
		newEntity("ordm", (char) 186);
		newEntity("raquo", (char) 187);
		newEntity("frac14", (char) 188);
		newEntity("frac12", (char) 189);
		newEntity("frac34", (char) 190);
		newEntity("iquest", (char) 191);
		newEntity("Agrave", (char) 192);
		newEntity("Aacute", (char) 193);
		newEntity("Acirc", (char) 194);
		newEntity("Atilde", (char) 195);
		newEntity("Auml", (char) 196);
		newEntity("Aring", (char) 197);
		newEntity("AElig", (char) 198);
		newEntity("Ccedil", (char) 199);
		newEntity("Egrave", (char) 200);
		newEntity("Eacute", (char) 201);
		newEntity("Ecirc", (char) 202);
		newEntity("Euml", (char) 203);
		newEntity("Igrave", (char) 204);
		newEntity("Iacute", (char) 205);
		newEntity("Icirc", (char) 206);
		newEntity("Iuml", (char) 207);
		newEntity("ETH", (char) 208);
		newEntity("Ntilde", (char) 209);
		newEntity("Ograve", (char) 210);
		newEntity("Oacute", (char) 211);
		newEntity("Ocirc", (char) 212);
		newEntity("Otilde", (char) 213);
		newEntity("Ouml", (char) 214);
		newEntity("times", (char) 215);
		newEntity("Oslash", (char) 216);
		newEntity("Ugrave", (char) 217);
		newEntity("Uacute", (char) 218);
		newEntity("Ucirc", (char) 219);
		newEntity("Uuml", (char) 220);
		newEntity("Yacute", (char) 221);
		newEntity("THORN", (char) 222);
		newEntity("szlig", (char) 223);
		newEntity("agrave", (char) 224);
		newEntity("aacute", (char) 225);
		newEntity("acirc", (char) 226);
		newEntity("atilde", (char) 227);
		newEntity("auml", (char) 228);
		newEntity("aring", (char) 229);
		newEntity("aelig", (char) 230);
		newEntity("ccedil", (char) 231);
		newEntity("egrave", (char) 232);
		newEntity("eacute", (char) 233);
		newEntity("ecirc", (char) 234);
		newEntity("euml", (char) 235);
		newEntity("igrave", (char) 236);
		newEntity("iacute", (char) 237);
		newEntity("icirc", (char) 238);
		newEntity("iuml", (char) 239);
		newEntity("eth", (char) 240);
		newEntity("ntilde", (char) 241);
		newEntity("ograve", (char) 242);
		newEntity("oacute", (char) 243);
		newEntity("ocirc", (char) 244);
		newEntity("otilde", (char) 245);
		newEntity("ouml", (char) 246);
		newEntity("divide", (char) 247);
		newEntity("oslash", (char) 248);
		newEntity("ugrave", (char) 249);
		newEntity("uacute", (char) 250);
		newEntity("ucirc", (char) 251);
		newEntity("uuml", (char) 252);
		newEntity("yacute", (char) 253);
		newEntity("thorn", (char) 254);
		newEntity("yuml", (char) 255);

		// Special
		newEntity("quot", (char) 34);
		newEntity("apos", (char) 39);
		newEntity("amp", (char) 38);
		newEntity("lt", (char) 60);
		newEntity("gt", (char) 62);
		newEntity("OElig", (char) 338);
		newEntity("oelig", (char) 339);
		newEntity("Scaron", (char) 352);
		newEntity("scaron", (char) 353);
		newEntity("Yuml", (char) 376);
		newEntity("circ", (char) 710);
		newEntity("tilde", (char) 732);
		newEntity("ensp", (char) 8194);
		newEntity("emsp", (char) 8195);
		newEntity("thinsp", (char) 8201);
		newEntity("zwnj", (char) 8204);
		newEntity("zwj", (char) 8205);
		newEntity("lrm", (char) 8206);
		newEntity("rlm", (char) 8207);
		newEntity("ndash", (char) 8211);
		newEntity("mdash", (char) 8212);
		newEntity("lsquo", (char) 8216);
		newEntity("rsquo", (char) 8217);
		newEntity("sbquo", (char) 8218);
		newEntity("ldquo", (char) 8220);
		newEntity("rdquo", (char) 8221);
		newEntity("bdquo", (char) 8222);
		newEntity("dagger", (char) 8224);
		newEntity("Dagger", (char) 8225);
		newEntity("permil", (char) 8240);
		newEntity("lsaquo", (char) 8249);
		newEntity("rsaquo", (char) 8250);
		newEntity("euro", (char) 8364);

		// Symbols
		newEntity("fnof", (char) 402);
		newEntity("Alpha", (char) 913);
		newEntity("Beta", (char) 914);
		newEntity("Gamma", (char) 915);
		newEntity("Delta", (char) 916);
		newEntity("Epsilon", (char) 917);
		newEntity("Zeta", (char) 918);
		newEntity("Eta", (char) 919);
		newEntity("Theta", (char) 920);
		newEntity("Iota", (char) 921);
		newEntity("Kappa", (char) 922);
		newEntity("Lambda", (char) 923);
		newEntity("Mu", (char) 924);
		newEntity("Nu", (char) 925);
		newEntity("Xi", (char) 926);
		newEntity("Omicron", (char) 927);
		newEntity("Pi", (char) 928);
		newEntity("Rho", (char) 929);
		newEntity("Sigma", (char) 931);
		newEntity("Tau", (char) 932);
		newEntity("Upsilon", (char) 933);
		newEntity("Phi", (char) 934);
		newEntity("Chi", (char) 935);
		newEntity("Psi", (char) 936);
		newEntity("Omega", (char) 937);
		newEntity("alpha", (char) 945);
		newEntity("beta", (char) 946);
		newEntity("gamma", (char) 947);
		newEntity("delta", (char) 948);
		newEntity("epsilon", (char) 949);
		newEntity("zeta", (char) 950);
		newEntity("eta", (char) 951);
		newEntity("theta", (char) 952);
		newEntity("iota", (char) 953);
		newEntity("kappa", (char) 954);
		newEntity("lambda", (char) 955);
		newEntity("mu", (char) 956);
		newEntity("nu", (char) 957);
		newEntity("xi", (char) 958);
		newEntity("omicron", (char) 959);
		newEntity("pi", (char) 960);
		newEntity("rho", (char) 961);
		newEntity("sigmaf", (char) 962);
		newEntity("sigma", (char) 963);
		newEntity("tau", (char) 964);
		newEntity("upsilon", (char) 965);
		newEntity("phi", (char) 966);
		newEntity("chi", (char) 967);
		newEntity("psi", (char) 968);
		newEntity("omega", (char) 969);
		newEntity("thetasym", (char) 977);
		newEntity("upsih", (char) 978);
		newEntity("piv", (char) 982);
		newEntity("bull", (char) 8226);
		newEntity("hellip", (char) 8230);
		newEntity("prime", (char) 8242);
		newEntity("Prime", (char) 8243);
		newEntity("oline", (char) 8254);
		newEntity("frasl", (char) 8260);
		newEntity("weierp", (char) 8472);
		newEntity("image", (char) 8465);
		newEntity("real", (char) 8476);
		newEntity("trade", (char) 8482);
		newEntity("alefsym", (char) 8501);
		newEntity("larr", (char) 8592);
		newEntity("uarr", (char) 8593);
		newEntity("rarr", (char) 8594);
		newEntity("darr", (char) 8595);
		newEntity("harr", (char) 8596);
		newEntity("crarr", (char) 8629);
		newEntity("lArr", (char) 8656);
		newEntity("uArr", (char) 8657);
		newEntity("rArr", (char) 8658);
		newEntity("dArr", (char) 8659);
		newEntity("hArr", (char) 8660);
		newEntity("forall", (char) 8704);
		newEntity("part", (char) 8706);
		newEntity("exist", (char) 8707);
		newEntity("empty", (char) 8709);
		newEntity("nabla", (char) 8711);
		newEntity("isin", (char) 8712);
		newEntity("notin", (char) 8713);
		newEntity("ni", (char) 8715);
		newEntity("prod", (char) 8719);
		newEntity("sum", (char) 8721);
		newEntity("minus", (char) 8722);
		newEntity("lowast", (char) 8727);
		newEntity("radic", (char) 8730);
		newEntity("prop", (char) 8733);
		newEntity("infin", (char) 8734);
		newEntity("ang", (char) 8736);
		newEntity("and", (char) 8743);
		newEntity("or", (char) 8744);
		newEntity("cap", (char) 8745);
		newEntity("cup", (char) 8746);
		newEntity("int", (char) 8747);
		newEntity("there4", (char) 8756);
		newEntity("sim", (char) 8764);
		newEntity("cong", (char) 8773);
		newEntity("asymp", (char) 8776);
		newEntity("ne", (char) 8800);
		newEntity("equiv", (char) 8801);
		newEntity("le", (char) 8804);
		newEntity("ge", (char) 8805);
		newEntity("sub", (char) 8834);
		newEntity("sup", (char) 8835);
		newEntity("nsub", (char) 8836);
		newEntity("sube", (char) 8838);
		newEntity("supe", (char) 8839);
		newEntity("oplus", (char) 8853);
		newEntity("otimes", (char) 8855);
		newEntity("perp", (char) 8869);
		newEntity("sdot", (char) 8901);
		newEntity("lceil", (char) 8968);
		newEntity("rceil", (char) 8969);
		newEntity("lfloor", (char) 8970);
		newEntity("rfloor", (char) 8971);
		newEntity("lang", (char) 9001);
		newEntity("rang", (char) 9002);
		newEntity("loz", (char) 9674);
		newEntity("spades", (char) 9824);
		newEntity("clubs", (char) 9827);
		newEntity("hearts", (char) 9829);
		newEntity("diams", (char) 9830);
	}

}
