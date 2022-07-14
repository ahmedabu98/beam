# Generated by the gRPC Python protocol compiler plugin. DO NOT EDIT!
"""Client and server classes corresponding to protobuf-defined services."""
import grpc

from . import beam_runner_api_pb2 as org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_beam__runner__api__pb2


class TestStreamServiceStub(object):
    """Missing associated documentation comment in .proto file."""

    def __init__(self, channel):
        """Constructor.

        Args:
            channel: A grpc.Channel.
        """
        self.Events = channel.unary_stream(
                '/org.apache.beam.model.pipeline.v1.TestStreamService/Events',
                request_serializer=org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_beam__runner__api__pb2.EventsRequest.SerializeToString,
                response_deserializer=org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_beam__runner__api__pb2.TestStreamPayload.Event.FromString,
                )


class TestStreamServiceServicer(object):
    """Missing associated documentation comment in .proto file."""

    def Events(self, request, context):
        """A TestStream will request for events using this RPC.
        """
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')


def add_TestStreamServiceServicer_to_server(servicer, server):
    rpc_method_handlers = {
            'Events': grpc.unary_stream_rpc_method_handler(
                    servicer.Events,
                    request_deserializer=org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_beam__runner__api__pb2.EventsRequest.FromString,
                    response_serializer=org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_beam__runner__api__pb2.TestStreamPayload.Event.SerializeToString,
            ),
    }
    generic_handler = grpc.method_handlers_generic_handler(
            'org.apache.beam.model.pipeline.v1.TestStreamService', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))


 # This class is part of an EXPERIMENTAL API.
class TestStreamService(object):
    """Missing associated documentation comment in .proto file."""

    @staticmethod
    def Events(request,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            insecure=False,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.unary_stream(request, target, '/org.apache.beam.model.pipeline.v1.TestStreamService/Events',
            org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_beam__runner__api__pb2.EventsRequest.SerializeToString,
            org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_beam__runner__api__pb2.TestStreamPayload.Event.FromString,
            options, channel_credentials,
            insecure, call_credentials, compression, wait_for_ready, timeout, metadata)
