from testcontainers.postgres import PostgresContainer


def test_postgres():
    # with PostgresContainer("postgres:16-alpine", driver=None) as p:
    with PostgresContainer("postgres:16-alpine") as p:
        url = p.get_connection_url()
        print(url)
