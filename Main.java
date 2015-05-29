import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
	private final static String PAGE_SQL = "data/jawiki-20150512-page.sql";
	private final static String PAGELINKS_SQL = "data/jawiki-20150512-pagelinks.sql";
	private final static String CATEGORYLINKS_SQL = "data/jawiki-20150512-categorylinks.sql";

	private final static String TARGET_CATEGORY = "存命人物";

	private final static String RESULT_TSV = "data/result.tsv";

	private final static double DAMPING_FACTOR = 0.85;
	private final static int LOOP = 100;
	private final static double CHANGE = 0.00005;

	private final static String PREFIX_PATTERN = "INSERT INTO `%s` VALUES ";

	private static Map<String, Integer> dic;
	private static Map<Integer, String> dicRev;
	private static Map<Integer, List<Integer>> link;
	private static Map<Integer, Double> result;
	private static HashSet<Integer> people;

	public static void main(String[] args) throws Exception {
		dic = new HashMap<String, Integer>();
		dicRev = new HashMap<Integer, String>();
		link = new HashMap<Integer, List<Integer>>();
		result = new HashMap<Integer, Double>();
		people = new HashSet<Integer>();

		long start = System.currentTimeMillis();
		long end;

		System.out.println("Step 1: Parsing `page` table");

		parsePage();

		end = System.currentTimeMillis();

		System.out.println("Step 1: DONE, " + (end - start) + "ms");

		start = end;

		System.out.println("Step 2: Parsing `pagelinks` table");

		parsePageLinks();

		end = System.currentTimeMillis();

		System.out.println("Step 2: DONE, " + (end - start) + "ms");

		start = end;

		System.out.println("Step 3: Calculating PageRank");

		calc();

		end = System.currentTimeMillis();

		System.out.println("Step 3: DONE, " + (end - start) + "ms");

		start = end;

		System.out.println("Step 4: Parsing `categorylinks` table");

		parseCategoryLinks();

		end = System.currentTimeMillis();

		System.out.println("Step 4: DONE, " + (end - start) + "ms");

		start = end;

		System.out.println("Step 5: Sorting");

		sort();

		end = System.currentTimeMillis();

		System.out.println("Step 5: DONE, " + (end - start) + "ms");

	}

	private static void sort() throws Exception {
		// TreeMap に入れ直してソートを行う。Comparator を指定して降順に並び替える。
		TreeMap<Double, Integer> s = new TreeMap<Double, Integer>(new Comparator<Double>() {
			public int compare(Double a, Double b) {
				return a.compareTo(b) * -1;
			}
		});

		for (Entry<Integer, Double> e : result.entrySet()) {
			s.put(e.getValue(), e.getKey());
		}

		int i = 0;
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(RESULT_TSV), "UTF-8"));

		for (Entry<Double, Integer> e : s.entrySet()) {
			Integer id = e.getValue();
			String title = dicRev.get(id);
			double pageRank = e.getKey();

			if (i < 10 && people.contains(id.intValue())) {
				i++;
				System.out.printf("%2d: %.15f, %s\n", i, pageRank, title);
			}

			// double の有効桁数は 15 桁程度で、pageRank は 1 未満であるから、小数点以下 15 桁は信頼できる
			writer.printf("%d\t%.15f\t%s\n", id, pageRank, title);
		}

		writer.close();
	}

	private static void calc() {
		Map<Integer, Double> temp = new HashMap<Integer, Double>();

		int l = link.size();

		long start = System.currentTimeMillis();
		long end;

		for (Integer key : link.keySet()) {
			result.put(key, 1.0 / (double)l);
			temp.put(key, 0.0);
		}

		double change = 1.0;

		for (int i = 0; i < LOOP; i++) {
			System.out.println("Step 3: Loop " + (i + 1) + "/" + LOOP);

			if (change < CHANGE) {
				System.out.println("Step 3: Loop " + (i + 1) + " Skipped, change = " + change);
				continue;
			}

			change = 0.0;

			for (Integer key : link.keySet()) {
				List<Integer> out = link.get(key);

				for (int j = 0; j < out.size(); j++) {
					Integer id = out.get(j);

					double d = temp.containsKey(id) ? temp.get(id) : 0.0;

					d += result.get(key) / (double)out.size();

					temp.put(id, d);
				}
			}

			double total = 0.0;

			for (Integer key : link.keySet()) {
				double d = temp.get(key);

				d = (DAMPING_FACTOR / (double)l) + (1.0 - DAMPING_FACTOR) * d;

				temp.put(key, d);

				change += Math.abs(result.get(key) - d);

				result.put(key, temp.get(key));
				temp.put(key, 0.0);

				total += result.get(key);
			}

			for (Integer key : result.keySet()) {
				double d = result.get(key);

				d /= total;

				result.put(key, d);
			}

			end = System.currentTimeMillis();

			System.out.println("Step 3: Loop " + (i + 1) + " DONE, " + (end - start) + "ms");

			start = end;
		}
	}

	private static void parseCategoryLinks() throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(CATEGORYLINKS_SQL))));

		String line;
		int cnt = 0;

		while((line = reader.readLine()) != null) {
			List<List<String>> list = parse("categorylinks", line, "\\((\\d+),\\'(.+?)\\',[^\\)]+?\\)");

			if (list == null) {
				continue;
			}

			int l = list.size();

			for (int i = 0; i < l; i++) {
				cnt++;

				List<String> el = list.get(i);
				Integer id = Integer.parseInt(el.get(0));
				String category = el.get(1);

				if (!TARGET_CATEGORY.equals(category)) {
					continue;
				}

				people.add(id);
			}

			System.out.printf("\rStep 4: Parsed %d objects. Found %d people.", cnt, people.size());
		}

		System.out.printf("\n");

		reader.close();
	}

	private static void parsePage() throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(PAGE_SQL))));

		String line;
		int cnt = 0;

		while((line = reader.readLine()) != null) {
			List<List<String>> list = parse("page", line, "\\((\\d+),(\\d),\\'(.+?)\\',.+?\\)");

			if (list == null) {
				continue;
			}

			int l = list.size();

			for (int i = 0; i < l; i++) {
				cnt++;

				List<String> el = list.get(i);
				String namespace = el.get(1);

				// namespace が 0 でない -> 通常の記事ページではない
				if (Integer.parseInt(namespace) != 0) {
					continue;
				}

				String id = el.get(0);
				String title = el.get(2);

				// ID からタイトルを引ける Map と、その逆の Map を作る
				dic.put(title, Integer.parseInt(id));
				dicRev.put(Integer.parseInt(id), title);
			}

			System.out.printf("\rStep 1: Parsed %d objects.", cnt);
		}

		System.out.printf("\n");

		reader.close();
	}

	private static void parsePageLinks() throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(PAGELINKS_SQL))));

		String line;
		int cnt = 0;

		while((line = reader.readLine()) != null) {
			List<List<String>> list = parse("pagelinks", line, "\\((\\d+),(\\d+),\\'(.+?)\\',(\\d+)\\)");

			if (list == null) {
				continue;
			}

			int l = list.size();

			for (int i = 0; i < l; i++) {
				cnt++;

				List<String> el = list.get(i);

				if (el.size() < 4) {
					System.out.println(el);
					continue;
				}

				String fromId = el.get(0);
				String ns1 = el.get(1);
				String toTitle = el.get(2);
				String ns2 = el.get(3);

				// namespace が 0 でない -> 通常の記事ページではない
				if (!(Integer.parseInt(ns1) == 0 && Integer.parseInt(ns2) == 0)) {
					continue;
				}

				if (!dic.containsKey(toTitle)) {
					continue;
				}

				int toId = dic.get(toTitle);
				int fromIdInt = Integer.parseInt(fromId);

				if (!link.containsKey(fromIdInt)) {
					link.put(fromIdInt, new ArrayList<Integer>());
				}

				link.get(fromIdInt).add(toId);
			}

			System.out.printf("\rStep 2: Parsed %d objects.", cnt);
		}

		System.out.printf("\n");

		reader.close();
	}

	private static List<List<String>> parse(String table, String line, String pattern) throws Exception {
		final String PREFIX = String.format(PREFIX_PATTERN, table);

		List<List<String>> ret = new ArrayList<List<String>>();
		List<String> temp = new ArrayList<String>();

		Pattern p = Pattern.compile(pattern);

		if (line.length() < PREFIX.length() || line.startsWith("--")) {
			return null;
		}

		if (!line.substring(0, PREFIX.length()).equals(PREFIX)) {
			return null;
		}

		// 正規表現による INSERT 文のパースを行う
		Matcher m = p.matcher(line);

		while(m.find()) {
			temp.clear();

			for (int i = 1; i < m.groupCount() + 1; i++) {
				temp.add(m.group(i));
			}

			ret.add(new ArrayList<String>(temp));
		}

		return ret;
	}
}
