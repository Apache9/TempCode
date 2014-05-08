package com.github.apache9.hbase;

import java.io.IOException;
import java.util.Map;
import java.util.NavigableSet;

import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.coprocessor.CoprocessorService;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.protobuf.ResponseConverter;
import org.apache.hadoop.hbase.protobuf.generated.ClientProtos.GetRequest;
import org.apache.hadoop.hbase.protobuf.generated.ClientProtos.GetResponse;
import org.apache.hadoop.hbase.regionserver.HRegion;
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
            for (Map.Entry<byte[], NavigableSet<byte[]>> family: originGet.getFamilyMap().entrySet()) {
                indexGet.addColumn(family.getKey(), Bytes.toBytes("pk"));
            }
            
        } catch (IOException e) {
            ResponseConverter.setControllerException(controller, e);
        }

    }
}
