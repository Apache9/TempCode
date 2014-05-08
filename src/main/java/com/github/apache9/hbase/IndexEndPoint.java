package com.github.apache9.hbase;

import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;

import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.coprocessor.CoprocessorService;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.protobuf.ResponseConverter;
import org.apache.hadoop.hbase.protobuf.generated.ClientProtos.GetRequest;
import org.apache.hadoop.hbase.protobuf.generated.ClientProtos.GetResponse;
import org.apache.hadoop.hbase.regionserver.HRegion;
import org.apache.hadoop.hbase.regionserver.KeyPrefixRegionSplitPolicy;
import org.apache.hadoop.hbase.util.Bytes;

import com.github.apache9.hbase.HBaseTestProtos.IndexService;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;

/**
 * @author zhangduo
 */
public class IndexEndPoint extends IndexService implements CoprocessorService,
        Coprocessor {

    private RegionCoprocessorEnvironment env;

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        this.env = (RegionCoprocessorEnvironment) env;
    }

    @Override
    public void stop(CoprocessorEnvironment env) throws IOException {}

    @Override
    public Service getService() {
        return this;
    }

    @Override
    public void get(RpcController controller, GetRequest request,
            RpcCallback<GetResponse> done) {
        HRegion region = env.getRegion();
        Get originGet;
        try {
            originGet = ProtobufUtil.toGet(request.getGet());
            byte[] row = originGet.getRow();
            Get indexGet = new Get(row);
            byte[] family = originGet.getFamilyMap().keySet().iterator().next();
            indexGet.addColumn(family, Bytes.toBytes("pk"));
            Result indexResult = region.get(indexGet);
            NavigableMap<Long, byte[]> version2PrimaryKeyMap = indexResult
                    .getMap().get(family).get(Bytes.toBytes("pk"));
            if (version2PrimaryKeyMap.isEmpty()) {
                done.run(GetResponse.newBuilder().build());
                return;
            }
            Map.Entry<Long, byte[]> versionAndPrimaryKey = version2PrimaryKeyMap
                    .entrySet().iterator().next();
            HTableDescriptor desc = region.getTableDesc();
            int prefixLength = Integer.parseInt(desc
                    .getValue(KeyPrefixRegionSplitPolicy.PREFIX_LENGTH_KEY));
            String textRow = Bytes.toString(row).substring(0, prefixLength)
                    + "_data_"
                    + Bytes.toString(versionAndPrimaryKey.getValue());
            System.out.println(textRow);
            Get dataGet = new Get(Bytes.toBytes(textRow));
            dataGet.setTimeStamp(versionAndPrimaryKey.getKey());
            for (byte[] qualifier: originGet.getFamilyMap().get(family)) {
                dataGet.addColumn(family, qualifier);
            }
            Result dataResult = region.get(dataGet);
            done.run(GetResponse.newBuilder()
                    .setResult(ProtobufUtil.toResult(dataResult)).build());
        } catch (IOException e) {
            ResponseConverter.setControllerException(controller, e);
        }

    }
}
