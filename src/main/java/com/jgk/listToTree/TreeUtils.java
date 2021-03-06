package com.jgk.listToTree;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class TreeUtils {


    /**
     * @param parentIdProperty 业务list 中的对象的pId属性
     * @param idProperty       业务list 中的对象的id属性
     *                         //     * @param originList       业务list     包含父子级关系
     * @param <T>              业务对象
     * @return
     */

    //根据list直接转为 Json
    public static <T> JSONArray listToJsonArray(String parentIdProperty, String idProperty, List<T> nodeList) {
        List<String> ppidList = getPpidList(parentIdProperty, idProperty, nodeList);
        JSONArray array = new JSONArray();
        return getJsonTree(parentIdProperty, idProperty, nodeList, null);
    }
    public static <T> JSONArray getJsonTree(String parentIdProperty, String idProperty, List<T> nodeList, String pid) {
        JSONArray jsonArray = new JSONArray();
        for (T t : nodeList) {
            String realPid = getValueByProperty(parentIdProperty, t);
            //是否是父级节点，是的话递归去看孩子，不是返回此时的jsonArray
            if (StringUtils.isEmpty(realPid) && StringUtils.isEmpty(pid) || StringUtils.isNotEmpty(realPid) && realPid.equals(pid)) {
                String id = getValueByProperty(idProperty, t);
                JSONArray childArray = getJsonTree(parentIdProperty, idProperty, nodeList, id);
                JSONObject object = JSONObject.parseObject(JSONObject.toJSONString(t));
                object.put("nodes", childArray);
                jsonArray.add(object);
            }
        }
        return jsonArray;
    }


    //转化为根节点为空的树
    public static <T> Node<T> listToTree(String parentIdProperty, String idProperty, List<T> originList) {
        List<Node<T>> nodeList = new ArrayList<>();
        //业务集合转化为模型集合
        for (T t : originList) {
            Node<T> node = new Node<>();
            node.setTreeObj(t);
            nodeList.add(node);
        }
        Node<T> root = new Node<>();
        root.setNodeList(getTree(parentIdProperty, idProperty, nodeList, null));
        return root;
    }
    //递归得到树(多棵树)
    public static<T> List<Node<T>> getTree(String parentIdProperty, String idProperty, List<Node<T>> nodeList, String pid) {
        List<Node<T>> list = new ArrayList<>();
        for (Node<T> node : nodeList) {
            String realPid = getValueByProperty(parentIdProperty, node.getTreeObj());
            if (StringUtils.isEmpty(realPid) && StringUtils.isEmpty(pid) || StringUtils.isNotEmpty(realPid) && realPid.equals(pid)) {
                String id = getValueByProperty(idProperty, node.getTreeObj());
                List<Node<T>> nodes = getTree(parentIdProperty, idProperty, nodeList, id);
                node.setNodeList(nodes);
                list.add(node);
            }
        }
        return list;
    }

//  非递归
    public static <T> Node<T> listToTreeBy(String parentIdProperty, String idProperty, List<T> originList) {
        Node<T> root = new Node<>();
        try {
            List<Node<T>> nodeList = new ArrayList<>();
            //添加空集合
            nodeList.add(root);
//            业务集合转化为模型集合
            for (T t : originList) {
                Node<T> node = new Node<>();
                node.setTreeObj(t);
                nodeList.add(node);
            }
            //遍历模型集合
            for (Node<T> node : nodeList) {
                List<Node<T>> nodeChilds = new ArrayList<>();
                for (Node<T> temp : nodeList) {
                    //排除根节点
                    if (temp.getTreeObj() == null) {
                        continue;
                    }
                    //获得temp父节点
                    String pid = getValueByProperty(parentIdProperty, temp.getTreeObj());
//                    pid可能为null     空字符串     0   -1    或者写成通用的
                    //node根节点，
                    if (node.getTreeObj() == null) {
                        //如果上级菜单为空字符串，0，-1,
                        //temp 没有父节点 即为一级菜单
                        if (StringUtils.isEmpty(pid) || "0".equals(pid) || "-1".equals(pid)) {
                            nodeChilds.add(temp);
                        }
                        //不是根节点
                    } else {
                        //获得node节点id
                        //id判断为空

                        String id = getValueByProperty(idProperty, node.getTreeObj());
                        //node节点id与temp父节点id相同
                        if (StringUtils.isNotEmpty(pid) && pid.equals(id)) {
                            nodeChilds.add(temp);
                        }
                    }
                }
                node.setNodeList(nodeChilds);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return root;
    }



    //由树转为Json(多棵树 转为 JsonArray)
    public  static <T> JSONArray treeToJsonArray(List<Node<T>> list){
        JSONArray jsonArray = new JSONArray();
        for (Node<T> n: list) {
            JSONObject object = JSONObject.parseObject(JSON.toJSONString(n.getTreeObj()));
            object.put("nodes", jsonArray);
            object.put("nodes",treeToJsonArray(n.getNodeList()));
            jsonArray.add(object);
        }
        return jsonArray;
    }

    //根节点不为空的树 转为 Json
    public static<T> JSONObject treeToJsonObject(Node<T> root) {
        JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(root.getTreeObj()));
        JSONArray jsonArray = new JSONArray();
        jsonObject.put("childList", jsonArray);
        if (root.getNodeList() != null) {
            for (Node node : root.getNodeList()) {
                JSONObject object = treeToJsonObject(node);
                jsonArray.add(object);
            }
        }
        return jsonObject;
    }

    //根据原始数据的list得到根节点的id
    public static <T> List<String> getPpidList(String parentIdProperty, String idProperty, List<T> list){
        List<String> ppidList = new ArrayList<>();
        Set<String> idSet = new TreeSet<>();
        Set<String> pidSet = new TreeSet<>();
        for (T t : list) {
            String id = getValueByProperty(idProperty, t);
            idSet.add(id);
            String pid = getValueByProperty(parentIdProperty, t);
            if(StringUtils.isNotEmpty(pid)){
                pidSet.add(pid);
            }
        }
        for(String pid:pidSet){
            Boolean flag = true;
            for (String id: idSet) {
                if(id.equals(pid)){
                    flag = false;
                    break;
                }
            }
            if(flag){
                ppidList.add(pid);
            }
        }
        ppidList.add("");
        ppidList.add(null);
        return ppidList;
    }




    //根据属性名 得到属性值
    private static <T> String getValueByProperty(String parentIdProperty, T node) {
        Field field = null;
        try {
            field = node.getClass().getDeclaredField(parentIdProperty);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        field.setAccessible(true);
        String id = null;
        try {
            if ("int".equals(field.getType().getName())) {
                id = field.get(node) + "";
            } else {
                id = (String) field.get(node);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return id;
    }


    public void DepthOrder(TreeDepthFunc depthFunc, Node root, String parentIdProperty) throws NoSuchFieldException, IllegalAccessException {
        if (root.getTreeObj() != null) {
            Field fpid = null;

            fpid = root.getTreeObj().getClass().getDeclaredField(parentIdProperty);
            fpid.setAccessible(true);
            String pid = null;
            pid = fpid.get(root.getTreeObj()) + "";

            depthFunc.run(pid);
            System.out.println(root.getTreeObj());
            if (root.getNodeList() != null) {
                for (Object node : root.getNodeList()) {
                    DepthOrder(depthFunc, (Node) node, parentIdProperty);
                }
            }
        } else {
            for (Object node : root.getNodeList()) {
                DepthOrder(depthFunc, (Node) node, parentIdProperty);
            }
        }
    }
}
