package compiler.ast;

import compiler.Parser;

abstract public class Node {
    public BlockNode currentBlock;

    Node() {
        this.currentBlock = Parser.currentBlock;
    }
}