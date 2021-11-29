package org.sunbird.cb.hubservices.serviceimpl;

import io.micrometer.core.instrument.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.sunbird.cb.hubservices.exception.GraphException;
import org.sunbird.cb.hubservices.exception.ValidationException;
import org.sunbird.cb.hubservices.model.NodeV2;
import org.sunbird.cb.hubservices.service.INodeService;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.hubservices.dao.IGraphDao;

import java.util.*;

@Service
public class NodeService implements INodeService {

    private Logger logger = LoggerFactory.getLogger(NodeService.class);

    @Autowired
    private IGraphDao graphDao;

    @Override
    public Boolean connect(NodeV2 from, NodeV2 to, Map<String, String> relationProperties) {

        Boolean flag = Boolean.FALSE;
        if (Objects.isNull(from) || Objects.isNull(to) || CollectionUtils.isEmpty(relationProperties)) {
            throw new ValidationException("Node(s) or relation properties cannot be empty");
        }
        try {
            graphDao.upsertNode(from);
            graphDao.upsertNode(to);
            graphDao.upsertRelation(from.getId(), to.getId(), relationProperties);
            flag = Boolean.TRUE;
            logger.info("user connection successful");

        } catch (GraphException d) {
            logger.error("node connection failed : {}", d);

        }
        return flag;
    }

    @Override
    public List<NodeV2> getNodeByOutRelation(String identifier, Map<String, String> relationProperties, int offset, int size) {

        checkParams(identifier, relationProperties);

        return getNodesWith(identifier, relationProperties, Constants.DIRECTION.OUT, offset, size, null);
    }

    @Override
    public List<NodeV2> getNodeByInRelation(String identifier, Map<String, String> relationProperties, int offset, int size) {

        checkParams(identifier, relationProperties);

        return getNodesWith(identifier, relationProperties, Constants.DIRECTION.IN, offset, size, null);
    }

    @Override
    public List<NodeV2> getAllNodes(String identifier, Map<String, String> relationProperties, int offset, int size) {

        checkParams(identifier, relationProperties);

        return getNodesWith(identifier, relationProperties, null, offset, size, Arrays.asList(Constants.Graph.ID.getValue()));
    }

    @Override
    public int getNodesCount(String identifier, Map<String, String> relationProperties, Constants.DIRECTION direction) {
        int count = 0;
        if (StringUtils.isEmpty(identifier)) {
            throw new ValidationException("identifier or relation properties cannot be empty");
        }
        try {
            count = graphDao.getNeighboursCount(identifier, relationProperties, direction);

        } catch (GraphException e) {
            logger.error("Nodes count failed: {}", e);
        }
        return count;
    }


    @Override
    public List<NodeV2> getNodeNextLevel(String identifier, Map<String, String> relationProperties, int offset, int size) {

        checkParams(identifier, relationProperties);

        List<NodeV2> nodes = new ArrayList<>();
        for (NodeV2 n : getNodesWith(identifier, relationProperties, Constants.DIRECTION.OUT, offset, size,null)) {
            nodes.addAll(getNodesWith(n.getId(), relationProperties, Constants.DIRECTION.OUT, offset, size, null));
        }
        return nodes;
    }

    private List<NodeV2> getNodesWith(String identifier, Map<String, String> relationProperties, Constants.DIRECTION direction, int offset, int size, List<String> attr) {
        List<NodeV2> nodes = Collections.emptyList();
        try {
            nodes = graphDao.getNeighbours(identifier, relationProperties, direction, offset, size, attr);
        } catch (GraphException d) {
            logger.error(" Fetching user nodes for relations failed : {}", d);
        }
        return nodes;


    }

    private void checkParams(String identifier, Map<String, String> relationProperties) {
        if (StringUtils.isEmpty(identifier) || CollectionUtils.isEmpty(relationProperties)) {
            throw new ValidationException("identifier or relation properties cannot be empty");
        }
    }

}
