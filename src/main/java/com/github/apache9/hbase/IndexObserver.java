package com.github.apache9.hbase;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.HRegion;
import org.apache.hadoop.hbase.regionserver.KeyPrefixRegionSplitPolicy;
import org.apache.hadoop.hbase.regionserver.wal.HLogKey;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * @author zhangduo
 */
public class IndexObserver extends BaseRegionObserver {

    @Override
    public void postPut(ObserverContext<RegionCoprocessorEnvironment> e,
            Put put, WALEdit edit, Durability durability) throws IOException {
        String rowKey = Bytes.toString(put.getRow());
        if (!rowKey.contains("_data_")) {
            return;
        }
        HRegion region = e.getEnvironment().getRegion();
        HTableDescriptor desc = region.getTableDesc();
        int prefixLength = Integer.parseInt(desc
                .getValue(KeyPrefixRegionSplitPolicy.PREFIX_LENGTH_KEY));
        String prefix = rowKey.substring(0, prefixLength);
        String primaryKey = rowKey.substring(prefixLength + "_data_".length());
        for (Map.Entry<byte[], List<Cell>> entry: put.getFamilyCellMap()
                .entrySet()) {
            for (Cell cell: entry.getValue()) {
                String cq = Bytes.toString(cell.getQualifierArray(),
                        cell.getQualifierOffset(), cell.getQualifierLength());
                String cv = Bytes.toString(cell.getValueArray(),
                        cell.getValueOffset(), cell.getValueLength());
                Put indexPut = new Put(Bytes.toBytes(prefix + "_index_" + cq
                        + "_" + cv));
                indexPut.add(entry.getKey(), Bytes.toBytes("pk"),
                        put.getTimeStamp(), Bytes.toBytes(primaryKey));
                indexPut.setDurability(Durability.SKIP_WAL);
                region.put(indexPut);
            }
        }
    }

    @Override
    public void postDelete(ObserverContext<RegionCoprocessorEnvironment> e,
            Delete delete, WALEdit edit, Durability durability)
            throws IOException {
        System.out.println("postDelete for " + delete + " of region "
                + e.getEnvironment().getRegion());
    }

    @Override
    public void postWALRestore(
            ObserverContext<RegionCoprocessorEnvironment> env,
            HRegionInfo info, HLogKey logKey, WALEdit logEdit)
            throws IOException {
        System.out.println("postWALRestore for " + logEdit + " of region "
                + env.getEnvironment().getRegion());
    }

}
