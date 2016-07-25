package test;

import java.util.ArrayList;
import java.util.List;

import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtVariable;


public class Abstraction {
	
	public Abstraction(CtMethod method) {
		this.method = method;
	}
	
	public String methodAbstraction() {
		List<CtParameter> parameters = method.getParameters();
//		int nParameters = parameters.size();
		List<Variable> listVariables = new ArrayList<Variable>();
		Variable varTemp;
		for(CtParameter p: parameters) {
			varTemp = new Variable(p.getSimpleName());
			System.out.println("var temp: " + varTemp);
			varTemp.initialize();
			listVariables.add(varTemp);
		}
		
		String f;
		
		f =  blockAbstraction(method.getBody(), listVariables);
		
		return f;
	}
	
	public String blockAbstraction(CtBlock block, List<Variable> listVariables) {
		if(block == null)
			return null;
		
		List<CtStatement> statements = block.getStatements();
		if(statements.isEmpty()) {
			return null;
		}
		
		String f = "1";
		String fStatement;
//		System.out.println("block: ");
		for(CtStatement s: statements) {
//			System.out.println(s + ", getClass: " + s.getClass());
			fStatement = statementAbstraction(s, listVariables);
			if(fStatement != null)
				f += " ^ " + fStatement;
		}
		
		return f;
	}
	
	public String statementAbstraction(CtStatement statement, List<Variable> listVariables) {
		String f = null;
		if(statement instanceof CtAssignment) {
			f = assignmentAbstraction((CtAssignment) statement, listVariables);
		}
		else if(statement instanceof CtUnaryOperator) {
			f = unaryOperator((CtUnaryOperator) statement, listVariables);
		}
		else if(statement instanceof CtIf) {
			f = ifAbstraction((CtIf) statement, listVariables); 
		} 
		else if(statement instanceof CtBlock) {
			f = blockAbstraction((CtBlock) statement, listVariables);
		} 
		else if(statement instanceof CtLocalVariable) {
			f = varDeclarationAbstraction( (CtLocalVariable) statement, 
												listVariables);
		}
		else if(statement instanceof CtFor) {
			f = forAbstraction((CtFor) statement, listVariables);
		}
		else if(statement instanceof CtReturn) {
			f = returnAbstraction((CtReturn) statement, listVariables);
		}	
		
		return f;
	}
	
	// chua hoan thien
	public String unaryOperator(CtUnaryOperator unaryOp, List<Variable> listVariables) {
		
		CtExpression operand = unaryOp.getOperand();
//		System.out.println("operand: " + operand + ", getclass: " + unaryOp.getClass());
		Variable variable = Variable.getVariable(operand.toString(), listVariables);
		UnaryOperatorKind operator = unaryOp.getKind();
		String binaryOperator = "";
		if(operator == UnaryOperatorKind.POSTDEC)
			binaryOperator = "-";
		if(operator == UnaryOperatorKind.PREDEC)
			binaryOperator = "-";
		if(operator == UnaryOperatorKind.POSTINC)
			binaryOperator = "+";
		if(operator == UnaryOperatorKind.PREINC)
			binaryOperator = "+";
		
		String f = variable.getValue() + binaryOperator + "1";
		variable.increase();
		f = "(" + variable.getValue() + "=" + f + ")";
			
		return f;
	}

	public String ifAbstraction(CtIf ifs, List<Variable> listVariables) {
		String f = "";
		
		CtExpression<Boolean> conditionExp = ifs.getCondition();
		String condition = expressionAbstraction(conditionExp, listVariables);
		
		CtStatement thenCtStatement = ifs.getThenStatement();
		CtStatement elseCtStatement = ifs.getElseStatement();
		
		List<Variable> thenList = Helper.copyList(listVariables);
		List<Variable> elseList = Helper.copyList(listVariables);
		
		String fThen = statementAbstraction(thenCtStatement, thenList);
		String fElse = statementAbstraction(elseCtStatement, elseList);
		
		Variable vThen, vElse;
		int indexOfVElse;
		int indexOfVThen;
		for(Variable v: listVariables) {
			vThen = Variable.getVariable(v.getName(), thenList);
			vElse = Variable.getVariable(v.getName(), elseList);
			indexOfVThen = vThen.getIndex();
			indexOfVElse = vElse.getIndex();
			
			if(indexOfVThen > indexOfVElse) {
				v.setIndex(indexOfVThen);
				fElse += "^" + v.getValue() + "=" + vElse.getValue();
			}
			else {
				v.setIndex(indexOfVElse);
				if(indexOfVElse > indexOfVThen) {
					fThen += "^" + v.getValue() + "=" + vThen.getValue();
				}
			}
				
		}
		
		f = "(" + condition + "->" + fThen + ") ^ ( ~" + condition + "->" + fElse + ")";
		
		return f;
	}
	
	// cho lap so lan mac dinh 
	public String forAbstraction(CtFor forloop, List<Variable> listVariables) {
		String f = "";
		
		List<CtStatement> forInit = forloop.getForInit();
		String fStatement;
		for(CtStatement s: forInit) {
			fStatement = statementAbstraction(s, listVariables);
			if(fStatement != null)
				f += " ^ " + fStatement;
		}
		if(!f.equals(""))
			f = f.substring(3);
		
		CtExpression<Boolean> exp = forloop.getExpression();
		String conditionLoop;
		
		List<CtStatement> forUpdate = forloop.getForUpdate();
		String fForUpdate = "";
		
		CtStatement body = forloop.getBody();
		String fBody;
		String fALoop = "";
		int nLoop;
		nLoop = defaultNumOfLoop;
		for(int i = 0; i < nLoop; i++) {
			conditionLoop = expressionAbstraction(exp, listVariables);
			fBody = statementAbstraction(body, listVariables);
			fForUpdate = "";
			for(CtStatement s: forUpdate) {
				fStatement = statementAbstraction(s, listVariables);
				if(fStatement != null)
					fForUpdate += " ^ " + fStatement;
			}
			
			fALoop = conditionLoop + "->" + "(" + fBody + fForUpdate + ")";
			f += " ^ " + "(" + fALoop + ")";
		}
		
		
		return f;
	}
	
	
	public String varDeclarationAbstraction(CtLocalVariable var, 
											List<Variable> listVariables) 
	{
		
//		System.out.println("VAR: " + var);
		String f = "";

		String variableName;
		variableName = var.getSimpleName();
//		System.out.println("variableName: " + variableName);
		Variable v = Variable.getVariable(variableName, listVariables);
		var.getAssignment();
		if(v == null) {
			v = new Variable(variableName);
			listVariables.add(v);
		}
		else {
			v.increase();
		}
		String fInitialize;
		CtExpression initializer = var.getAssignment();
		if(initializer != null) {
			fInitialize = v.getValue() + "=" + expressionAbstraction(initializer, listVariables);
			f += " ^ " + fInitialize; 
		
		}
		
		if(f.equals(""))
			return null;
		else
			return "(" + f.substring(3) + ")";	// bo " ^ " o dau
	}
	
	public String returnAbstraction(CtReturn ret, List<Variable> listVariables) {
		CtExpression retExp = ret.getReturnedExpression();
		String fRetExp = expressionAbstraction(retExp, listVariables);
		
		return "ret = " + fRetExp; 
	}
	
	public String expressionAbstraction(CtExpression exp, List<Variable> listVariables) {
		String f = null;
		
		if(exp instanceof CtBinaryOperator) {			
			f = binaryOperationAbstration((CtBinaryOperator) exp, listVariables);
		}
		else if(exp instanceof CtVariableAccess) {
			f = variableAccessAbstraction((CtVariableAccess) exp, listVariables);
		}
		else if(exp instanceof CtLiteral) {
			f = exp.toString();
		}
		
		return f;
	}
	
	public String assignmentAbstraction(CtAssignment ass, List<Variable> listVariables) {
		String f = "";
		CtExpression left = ass.getAssigned();
		CtExpression right = ass.getAssignment();
		
		Variable v = Variable.getVariable(left.toString(), listVariables);

		String leftHandSide = "N/A";
		String rightHandSide = expressionAbstraction(right, listVariables);
		if(v != null) {
			if(v.hasInitialized())
				v.increase();
			else
				v.initialize();
			leftHandSide = v.getValue();
		}
		
		f = "(" + leftHandSide + " = " + rightHandSide + ")";
		return f;
	}
	
	public String binaryOperationAbstration(CtBinaryOperator binOp, List<Variable> listVariables) {
//		System.out.println("BinOP: " + binOp);
		CtExpression left = binOp.getLeftHandOperand();
		CtExpression right = binOp.getRightHandOperand();
		
		String fLeft = expressionAbstraction(left, listVariables);
		String fRight = expressionAbstraction(right, listVariables);
		String operator = Helper.getBinaryOperator(binOp.getKind());
		
		return "(" + fLeft + operator + fRight + ")";
	}
	
	public String variableAccessAbstraction(CtVariableAccess var, List<Variable> listVariables) {
		Variable v = Variable.getVariable(var.toString(), listVariables);
		
		return v.getValue();
	}
	
	
	
	private CtMethod method;
	
	private int defaultNumOfLoop = 10;
}
