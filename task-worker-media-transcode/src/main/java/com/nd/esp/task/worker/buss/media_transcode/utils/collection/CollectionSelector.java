package com.nd.esp.task.worker.buss.media_transcode.utils.collection;
/**
 * 集合的选择器
 *
 * @author bifeng.liu
 */
public interface CollectionSelector {
    /**
     * 对数据对象进行选择
     *
     * @param data
     * @return
     */
    Object select(Object data);
}
