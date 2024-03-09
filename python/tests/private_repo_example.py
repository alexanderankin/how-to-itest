import pytest
from testcontainers.core.container import DockerContainer

REGISTRY_2_TAG = 'registry:2@sha256:83bb78d7b28f1ac99c68133af32c93e9a1c149bcd3cb6e683a3ee56e312f1c96'


def copy_alpine_to_localhost():
    pass


@pytest.fixture(autouse=True, scope='module')
def private_registry():
    """
    if test -f htpasswd ; then rm htpasswd; fi && \
        htpasswd -bcB htpasswd admin admin && \
        docker run --rm -it -p 5000:5000 \
            -e REGISTRY_AUTH_HTPASSWD_REALM=basic-realm \
            -e REGISTRY_LOG_ACCESSLOG_DISABLED=true \
            -e REGISTRY_LOG_LEVEL=WARN \
            -e REGISTRY_AUTH_HTPASSWD_PATH=/htpasswd \
            -v ./htpasswd:/htpasswd \
            -e REGISTRY_HTTP_SECRET='keyboard cat' \
            $img
    :return:
    """
    container = DockerContainer(REGISTRY_2_TAG)
    container.with_env('REGISTRY_AUTH_HTPASSWD_REALM', 'basic-realm')
    container.with_env('REGISTRY_LOG_ACCESSLOG_DISABLED', 'true')
    container.with_env('REGISTRY_LOG_LEVEL', 'WARN')
    container.with_env('REGISTRY_AUTH_HTPASSWD_PATH', '/htpasswd')
    container.with_env('REGISTRY_HTTP_SECRET', 'keyboard cat')
    container.with_env('', '')
    with container:
        container.get_docker_client().get_container(container_id=container._container.id).copy()
        yield


def test():
    print('hi')
