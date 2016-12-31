package nd.esp.service.lifecycle.services.recycles.v06.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nd.esp.service.lifecycle.educommon.models.ResContributeModel;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
import nd.esp.service.lifecycle.services.lifecycle.v06.LifecycleServiceV06;
import nd.esp.service.lifecycle.services.recycles.v06.RecycleServiceV06;
import nd.esp.service.lifecycle.support.enums.LifecycleStatus;
import nd.esp.service.lifecycle.utils.CollectionUtils;
import nd.esp.service.lifecycle.utils.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecycleServiceImplV06 implements RecycleServiceV06 {
    //加入回收站
    public static final String OPERATE_ADD = "add";
    //从回收站还原
    public static final String OPERATE_RESTORE = "restore";

    @Autowired
    @Qualifier(value = "lifecycleServiceV06")
    private LifecycleServiceV06 lifecycleService;

    @Autowired
    private JdbcTemplate jt;

    @Override
    public Map<String, Object> operateRecycleResource(String resType,
                                                      String resId, String operateType) {
        //由于不清楚具体子资源类型，按固定顺序查找，后续如果支持其它资源，将以下逻辑单独移出
        //根据套件查找子套件
        List<String> paramList = new ArrayList<String>();
        paramList.add(resId);
        List<String> ids1 = queryTargetsBySources(resType, paramList, IndexSourceType.AssetType.getName(), "$RA0502", operateType);

        if (CollectionUtils.isNotEmpty(ids1)) {
            paramList.addAll(ids1);
        }

        //根据子套件查找教学目标类型
        List<String> ids2 = queryTargetsBySources(IndexSourceType.AssetType.getName(), paramList, IndexSourceType.AssetType.getName(), "$RA0503", operateType);

        //根据教学目标类型查找教学目标
        if (CollectionUtils.isEmpty(ids2)) {
            ids2 = new ArrayList<String>();
            ids2.add(resId);
        }
        List<String> ids3 = queryTargetsBySources(IndexSourceType.AssetType.getName(), ids2, IndexSourceType.InstructionalObjectiveType.getName(), "$RA0204", operateType);

        if (CollectionUtils.isNotEmpty(ids2)) {
            paramList.addAll(ids2);
        }

        if (CollectionUtils.isNotEmpty(ids3)) {
            paramList.addAll(ids3);
        }
        operate(paramList, operateType);
        batchAddLifeCycle(IndexSourceType.AssetType.getName(), paramList, operateType);

        return null;
    }

    /**
     * 根据源资源id查找目标资源
     *
     * @param resType        源资源类型
     * @param sourceUuidList 源资源id集合
     * @param targetType     目标资源类型
     * @param targetCategory 目标资源分类维度
     * @param operateType    操作方式   add--加入回收站	restore--还原回收站资源
     * @return
     */
    private List<String> queryTargetsBySources(String resType, List<String> sourceUuidList, String targetType, String targetCategory, String operateType) {
        String sql = ""
                + "SELECT nd.identifier "
                + "FROM ndresource nd, "
                + "     resource_relations rr, "
                + "     resource_categories rc "
                + "WHERE rr.res_type='" + resType + "' "
                + "  AND rr.enable = 1"
                + "  AND rr.resource_target_type = '" + targetType + "' "
                + "  AND rr.source_uuid IN (:sourceUuidList) "
                + "  AND nd.primary_category = '" + targetType + "' "
                + "  AND rc.primary_category='" + targetType + "' "
                + "  AND rr.target = nd.identifier "
                + "  AND nd.identifier = rc.resource "
                + "  AND rc.taxOnCode = '" + targetCategory + "'";

        if (operateType.equals(OPERATE_ADD)) {
            sql += "  AND nd.enable = 1 ";
        } else if (operateType.equals(OPERATE_RESTORE)) {
            sql = sql + " AND nd.enable = 0 "
                    + "  AND nd.estatus = '" + LifecycleStatus.RECYCLED.getCode() + "'";
        }

        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("sourceUuidList", sourceUuidList);
        List<String> resultList = npjt.query(sql, params, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum)
                    throws SQLException {
                Map<String, Object> m = new HashMap<String, Object>();
                String identifier = rs.getString("identifier");
                return identifier;
            }
        });
        return resultList;
    }

    /**
     * 修改资源状态
     *
     * @param idList      资源id集合
     * @param operateType 操作方式
     */
    private void operate(List<String> idList, String operateType) {
        String sql = null;
        String ids = "'" + StringUtils.join(idList, "','") + "'";
        if (OPERATE_ADD.equals(operateType)) {
            sql = "update ndresource set enable = 0,estatus= '" + LifecycleStatus.RECYCLED.getCode() + "',last_update=" + System.currentTimeMillis() + " where identifier in (" + ids + ")";
        } else if (OPERATE_RESTORE.equals(operateType)) {
            sql = "update ndresource set enable = 1,estatus= '" + LifecycleStatus.AUDIT_WAITING.getCode() + "',last_update=" + System.currentTimeMillis() + " where identifier in (" + ids + ")";
        }
        jt.execute(sql);
    }

    private void batchAddLifeCycle(String resType, List<String> resIds, String operateType) {
        ResContributeModel cm = new ResContributeModel();
        if (OPERATE_ADD.equals(operateType)) {
            cm.setLifecycleStatus(LifecycleStatus.RECYCLED.getCode());
        } else if (OPERATE_RESTORE.equals(operateType)) {
            cm.setLifecycleStatus(LifecycleStatus.AUDIT_WAITING.getCode());
        }
        lifecycleService.addLifecycleStepBatch(resType, resIds, cm);
    }
}
