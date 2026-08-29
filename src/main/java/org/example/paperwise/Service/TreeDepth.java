package org.example.paperwise.Service;

public class TreeDepth {
    public static class TreeNode {
        int val;
        TreeNode firstChild;
        TreeNode nextSibling;
        public TreeNode(int val) {
            this.val = val;
        }
    }

    public int  getTreeDepth(TreeNode root) {
        if (root == null) {
            return 0;
        }
        return  Math.max(getTreeDepth(root.firstChild)+1, getTreeDepth(root.nextSibling));
    }
}
