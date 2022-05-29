import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.*;
import javax.swing.JFileChooser;

/**
 * The parser and interpreter. The top level parse function, a main method for
 * testing, and several utility methods are provided. You need to implement
 * parseProgram and all the rest of the parser.
 */
public class Parser {

	/**
	 * Top level parse method, called by the World
	 */
	static RobotProgramNode parseFile(File code) {
		Scanner scan = null;
		try {
			scan = new Scanner(code);

			// the only time tokens can be next to each other is
			// when one of them is one of (){},;
			scan.useDelimiter("\\s+|(?=[{}(),;])|(?<=[{}(),;])");

			RobotProgramNode n = parseProgram(scan); // You need to implement this!!!

			scan.close();
			return n;
		} catch (FileNotFoundException e) {
			System.out.println("Robot program source file not found");
		} catch (ParserFailureException e) {
			System.out.println("Parser error:");
			System.out.println(e.getMessage());
			scan.close();
		}
		return null;
	}

	/** For testing the parser without requiring the world */

	public static void main(String[] args) {
		if (args.length > 0) {
			for (String arg : args) {
				File f = new File(arg);
				if (f.exists()) {
					System.out.println("Parsing '" + f + "'");
					RobotProgramNode prog = parseFile(f);
					System.out.println("Parsing completed ");
					if (prog != null) {
						System.out.println("================\nProgram:");
						System.out.println(prog);
					}
					System.out.println("=================");
				} else {
					System.out.println("Can't find file '" + f + "'");
				}
			}
		} else {
			while (true) {
				JFileChooser chooser = new JFileChooser(".");// System.getProperty("user.dir"));
				int res = chooser.showOpenDialog(null);
				if (res != JFileChooser.APPROVE_OPTION) {
					break;
				}
				RobotProgramNode prog = parseFile(chooser.getSelectedFile());
				System.out.println("Parsing completed");
				if (prog != null) {
					System.out.println("Program: \n" + prog);
				}
				System.out.println("=================");
			}
		}
		System.out.println("Done");
	}

	// Useful Patterns

	static Pattern NUMPAT = Pattern.compile("-?\\d+"); // ("-?(0|[1-9][0-9]*)");
	static Pattern OPENPAREN = Pattern.compile("\\(");
	static Pattern CLOSEPAREN = Pattern.compile("\\)");
	static Pattern OPENBRACE = Pattern.compile("\\{");
	static Pattern CLOSEBRACE = Pattern.compile("\\}");
	static Pattern ACTION = Pattern.compile("move|turnL|turnR|takeFuel|wait|shieldOn|shieldOff|turnAround");
	static Pattern SENSOR = Pattern.compile("fuelLeft|oppLR|oppFB|numBarrels|barrelLR|barrelFB|wallDist");

	/**
	 * See assignment handout for the grammar.
	 */
	static RobotProgramNode parseProgram(Scanner s) {
		// THE PARSER GOES HERE
		ArrayList<RobotProgramNode> initNodeList = new ArrayList<RobotProgramNode>();
		while (s.hasNext()) {
			initNodeList.add(parseStatement(s));
		}
		return new ProgramNode(initNodeList);
	}

	// utility methods for the parser

	private static RobotProgramNode parseStatement(Scanner s) {
		if (s.hasNext(ACTION)) {
			RobotProgramNode action = parseAction(s);
			if (action != null) {
				require(";", "Action does not have a ';'", s);
				return action;
			}
		}

		if (s.hasNext("loop")) {
			RobotProgramNode rpn = parseLoop(s);
			if (rpn != null)
				return rpn;
		}

		if (s.hasNext("if")) {
			RobotProgramNode rpn = parseIf(s);
			if (rpn != null)
				return rpn;
		}

		if (s.hasNext("while")) {
			RobotProgramNode rpn = parseWhile(s);
			if (rpn != null)
				return rpn;
		}
		fail("Parsing failed, statement not recognized", s);
		return null;
	}

	private static RobotProgramNode parseLoop(Scanner s) {
		if (checkFor("loop", s)) {
			BlockNode blocknode = parseBlock(s);

			if (blocknode != null) {
				return blocknode;
			}
		}
		return null;
	}

	private static BlockNode parseBlock(Scanner s) {
		BlockNode node = new BlockNode();
		require(OPENBRACE, "Open Bracket", s);
		if (s.hasNext(CLOSEBRACE)) {
			fail("parsing failed, block is empty", s);
		}
		while (!s.hasNext(CLOSEBRACE)) {
			RobotProgramNode RPNNode = parseStatement(s);
			if (RPNNode != null) {
				node.getNodeList().add(RPNNode);
			} else {
				fail("Statement parsing failed!", s);
			}
		}
		require(CLOSEBRACE, "Close bracket", s);
		return node;
	}

	static RobotProgramNode parseWhile(Scanner s) {
		require("while", "Missing While!", s);
		require(OPENPAREN, "Missing opening parenthesis", s);
		RobotConditionNode condNode = parseCOND(s);
		require(CLOSEPAREN, "Missing closing parenthesis", s);
		BlockNode blockNode = parseBlock(s);
		RobotProgramNode whileNode = new whileNode(blockNode, condNode);

		if (whileNode != null) {
			System.out.println("returns the whileNode");
			return whileNode;
		}
		return null;
	}

	static RobotConditionNode parseCOND(Scanner s) {
		if (checkFor("gt", s)) {
			require(OPENPAREN, "Missing opening parenthesis", s);
			RobotConditionNode rcn = parseGtNode(s);
			require(CLOSEPAREN, "Missing closing parenthesis", s);
			return rcn;
		}

		if (checkFor("lt", s)) {
			require(OPENPAREN, "Missing opening parenthesis", s);
			RobotConditionNode rcn = parseLtNode(s);
			require(CLOSEPAREN, "Missing closing parenthesis", s);
			return rcn;
		}

		if (checkFor("eq", s)) {
			require(OPENPAREN, "Missing opening parenthesis", s);
			RobotConditionNode rcn = parseEqNode(s);
			require(CLOSEPAREN, "Missing closing parenthesis", s);
			return rcn;
		}
		return null;

	}

	static RobotConditionNode parseGtNode(Scanner s) {
		String sensor = require(SENSOR, "Missing sensor!", s);
		require(",", "Missing ','!", s);
		int x = requireInt(NUMPAT, "Missing a number!", s);
		return new gtNode(sensor, x);

	}

	static RobotConditionNode parseLtNode(Scanner s) {
		String sensor = require(SENSOR, "Missing sensor!", s);
		require(",", "Missing ','!", s);
		int x = requireInt(NUMPAT, "Missing a number!", s);
		return new ltNode(sensor, x);

	}

	static RobotConditionNode parseEqNode(Scanner s) {
		String sensor = require(SENSOR, "Missing sensor!", s);
		require(",", "Missing ','!", s);
		int x = requireInt(NUMPAT, "Missing a number!", s);
		return new eqNode(sensor, x);

	}

	private static RobotProgramNode parseIf(Scanner s) {
		System.out.println("tettt");
		require("if", "Missing if!", s);
		require(OPENPAREN, "Missing opening parenthesis", s);
		RobotConditionNode condNode = parseCOND(s);
		require(CLOSEPAREN, "Missing closing parenthesis", s);
		BlockNode ifblock = parseBlock(s);

		if (checkFor("else", s)) {
			BlockNode elseblock = parseBlock(s);
			return new ifNode(ifblock, condNode, elseblock);
		}

		RobotProgramNode ifNode = new ifNode(ifblock, condNode);
		if (ifNode != null) {
			return ifNode;
		}
		return null;
	}

	private static RobotProgramNode parseAction(Scanner s) {

		if (checkFor("turnL", s))
			return new TurnLNode();
		if (checkFor("turnR", s))
			return new TurnRNode();
		// Boolean waitParam = s.hasNext(Pattern.compile("wait\\(\\d*\\)"));
		// if(waitParam) return new
		// moveNode(Integer.valueOf(Pattern.compile("\\d*").matcher(s.next()).group(1)));
		if (checkFor("wait", s))
			return new waitNode();
		// Boolean moveParam = s.hasNext(Pattern.compile("move\\(\\d*\\)"));
		// if(moveParam) return new
		// moveNode(Integer.valueOf(Pattern.compile("\\d*").matcher(s.next()).group(1)));
		if (checkFor("move", s))
			return new moveNode();
		if (checkFor("takeFuel", s))
			return new takeFuelNode();
		if (checkFor("shieldOn", s))
			return new shieldOnNode();
		if (checkFor("shieldOff", s))
			return new shieldOffNode();
		if (checkFor("turnAround", s))
			return new turnAroundNode();
		return null;
	}

	static class gtNode implements RobotConditionNode {
		String sensor;
		int num;

		gtNode(String s, int i) {
			this.sensor = s;
			this.num = i;
		}

		@Override
		public boolean evaluate(Robot robot) {
			if (sensor.equals("fuelLeft")) {
				if (robot.getFuel() > num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("oppLR")) {
				if (robot.getOpponentLR() > num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("oppFB")) {
				if (robot.getOpponentFB() > num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("numBarrels")) {
				if (robot.numBarrels() > num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("barrelLR")) {
				if (robot.getClosestBarrelLR() > num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("barrelFB")) {
				if (robot.getClosestBarrelFB() > num) {
					return true;
				}
				return false;
			}
			if (sensor.equals("wallDist")) {
				if (robot.getDistanceToWall() > num) {
					return true;
				}
				return false;
			}
			return false;
		}

		public String toString() {
			return "wait";
		}
	}

	static class ltNode implements RobotConditionNode {

		String sensor;
		int num;

		ltNode(String s, int i) {
			this.sensor = s;
			this.num = i;
		}

		@Override
		public boolean evaluate(Robot robot) {
			if (sensor.equals("fuelLeft")) {

				if (robot.getFuel() < num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("oppLR")) {
				if (robot.getOpponentLR() < num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("oppFB")) {
				if (robot.getOpponentFB() < num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("numBarrels")) {
				if (robot.numBarrels() < num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("barrelLR")) {
				if (robot.getClosestBarrelLR() < num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("barrelFB")) {
				if (robot.getClosestBarrelFB() < num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("wallDist")) {
				if (robot.getDistanceToWall() < num) {
					return true;
				}
				return false;
			}
			return false;
		}

		public String toString() {
			return "wait";
		}
	}

	static class eqNode implements RobotConditionNode {
		String sensor;
		int num;

		eqNode(String s, int i) {
			this.sensor = s;
			this.num = i;
		}

		@Override
		public boolean evaluate(Robot robot) {
			if (sensor.equals("fuelLeft")) {
				if (robot.getFuel() == num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("oppLR")) {
				if (robot.getOpponentLR() == num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("oppFB")) {
				if (robot.getOpponentFB() == num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("numBarrels")) {
				if (robot.numBarrels() == num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("barrelLR")) {
				if (robot.getClosestBarrelLR() == num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("barrelFB")) {
				if (robot.getClosestBarrelFB() == num) {
					return true;
				}
				return false;
			}

			if (sensor.equals("wallDist")) {
				if (robot.getDistanceToWall() == num) {
					return true;
				}
				return false;
			}
			return false;
		}

		public String toString() {
			return "wait";
		}
	}

	/**
	 * Report a failure in the parser.
	 */
	static void fail(String message, Scanner s) {
		String msg = message + "\n   @ ...";
		for (int i = 0; i < 5 && s.hasNext(); i++) {
			msg += " " + s.next();
		}
		throw new ParserFailureException(msg + "...");
	}

	/**
	 * Requires that the next token matches a pattern if it matches, it consumes
	 * and returns the token, if not, it throws an exception with an error
	 * message
	 */
	static String require(String p, String message, Scanner s) {
		if (s.hasNext(p)) {
			return s.next();
		}
		fail(message, s);
		return null;
	}

	static String require(Pattern p, String message, Scanner s) {
		if (s.hasNext(p)) {
			return s.next();
		}
		fail(message, s);
		return null;
	}

	/**
	 * Requires that the next token matches a pattern (which should only match a
	 * number) if it matches, it consumes and returns the token as an integer if
	 * not, it throws an exception with an error message
	 */
	static int requireInt(String p, String message, Scanner s) {
		if (s.hasNext(p) && s.hasNextInt()) {
			return s.nextInt();
		}
		fail(message, s);
		return -1;
	}

	static int requireInt(Pattern p, String message, Scanner s) {
		if (s.hasNext(p) && s.hasNextInt()) {
			return s.nextInt();
		}
		fail(message, s);
		return -1;
	}

	/**
	 * Checks whether the next token in the scanner matches the specified
	 * pattern, if so, consumes the token and return true. Otherwise returns
	 * false without consuming anything.
	 */
	static boolean checkFor(String p, Scanner s) {
		if (s.hasNext(p)) {
			s.next();
			return true;
		} else {
			return false;
		}
	}

	static boolean checkFor(Pattern p, Scanner s) {
		if (s.hasNext(p)) {
			s.next();
			return true;
		} else {
			return false;
		}
	}

}

// ----------------------------------------------------------------
// ACTIONS -------------------------------------------------------
// ----------------------------------------------------------------

// You could add the node classes here, as long as they are not declared public
// (or private)
class TurnLNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.turnLeft();
	}

	public String toString() {
		return "turnL";
	}

}

class TurnRNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.turnRight();
	}

	public String toString() {
		return "turnR";
	}

}

class moveNode implements RobotProgramNode {
	@Override
	public void execute(Robot robot) {
		robot.move();
	}

	public String toString() {
		return "move";
	}

}

class takeFuelNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.takeFuel();
	}

	public String toString() {
		return "takeFuel";
	}

}

class waitNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.idleWait();
	}

	public String toString() {
		return "wait";
	}
}

class LoopNode implements RobotProgramNode {
	BlockNode block;

	LoopNode(BlockNode block) {
		this.block = block;
	}

	@Override
	public void execute(Robot robot) {
		block.execute(robot);
	}

	public String toString() {
		return "loop" + this.block;
	}
}

class whileNode implements RobotProgramNode {
	BlockNode blocknode;
	RobotConditionNode condnode;

	whileNode(BlockNode blocknode, RobotConditionNode condnode) {
		this.blocknode = blocknode;
		this.condnode = condnode;
	}

	@Override
	public void execute(Robot robot) {
		while (condnode.evaluate(robot)) {
			blocknode.execute(robot);
		}
	}

	public String toString() {
		return "while" + this.blocknode;
	}

}

class ifNode implements RobotProgramNode {
	BlockNode ifblock;
	BlockNode elseblock;
	RobotConditionNode condnode;

	ifNode(BlockNode block, RobotConditionNode rcn) {
		this.ifblock = block;
		this.condnode = rcn;
	}

	ifNode(BlockNode block, RobotConditionNode rcn, BlockNode block2) {
		this.ifblock = block;
		this.elseblock = block2;
		this.condnode = rcn;
	}

	@Override
	public void execute(Robot robot) {
		if (elseblock == null) {
			if (condnode.evaluate(robot)) {
				ifblock.execute(robot);
			}
		} else {
			if (condnode.evaluate(robot)) {
				ifblock.execute(robot);
			} else {
				elseblock.execute(robot);
			}
		}
	}

	public String toString() {
		if (elseblock == null) {
			return "if" + "(" + condnode.toString() + ")" + this.ifblock.toString();
		} else {
			return "if" + "(" + condnode.toString() + ")" + this.ifblock.toString() + " else " + elseblock.toString();
		}
	}

}

class BlockNode implements RobotProgramNode {
	ArrayList<RobotProgramNode> nodes = new ArrayList<RobotProgramNode>();

	@Override
	public void execute(Robot robot) {
		for (RobotProgramNode n : nodes) {
			n.execute(robot);
		}
	}

	public ArrayList<RobotProgramNode> getNodeList() {
		return this.nodes;
	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("{\n");
		for (RobotProgramNode n : this.nodes) {
			str.append("   " + n.toString() + "\n");
		}
		str.append("}");
		return str.toString();
	}
}

class ProgramNode implements RobotProgramNode {
	ArrayList<RobotProgramNode> nodes = new ArrayList<RobotProgramNode>();

	ProgramNode(ArrayList<RobotProgramNode> nodes) {
		this.nodes = nodes;
	}

	@Override
	public void execute(Robot robot) {
		for (RobotProgramNode n : nodes) {
			n.execute(robot);
		}

	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		for (RobotProgramNode n : this.nodes) {
			str.append(n.toString() + "\n");
		}
		return str.toString();
	}

}

class shieldOnNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.setShield(true);
	}

	public String toString() {
		return "shieldOn";
	}

}

class shieldOffNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.setShield(false);
	}

	public String toString() {
		return "shieldOff";
	}

}

class turnAroundNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.turnAround();
	}

	public String toString() {
		return "shieldOff";
	}

}
