import argparse, psycopg2, os

conn = psycopg2.connect(database="part2", user="vagrant")
conn.autocommit = True
db = conn.cursor()

def setup(test_file):
    """
    Loads the a csv edge file from the tests directory into the database
    """
    test_path = "{path}/tests/{test_file}".format(path=os.getcwd(), test_file=test_file)
    db.execute("""
        DROP SCHEMA PUBLIC CASCADE; CREATE SCHEMA PUBLIC;
        CREATE TABLE original_edge (id SERIAL, src int, dst int, capacity int);
        CREATE TABLE edge (id SERIAL, src int, dst int, capacity int);
        COPY original_edge(src,dst,capacity) FROM %s DELIMITER ',' CSV HEADER;
        COPY edge(src,dst,capacity) FROM %s DELIMITER ',' CSV HEADER;
        """, (test_path, test_path))

    # We create a list of nodes based on the edges.
    # Thus every node in our graph is connected to an edge.
    db.execute("""
        SELECT DISTINCT id INTO node FROM
        (SELECT src as id FROM original_edge UNION SELECT dst as id FROM original_edge) AS tmp
        ORDER BY id;
    """)

    # `original_edge` is the data loaded from the edge file
    # `edge` additionally contains the residual edges
    db.execute("""
        INSERT INTO edge(src, dst, capacity)
          SELECT dst, src, 0 FROM original_edge O
          WHERE NOT EXISTS(SELECT * FROM original_edge E 
            WHERE E.src = O.dst AND E.dst = O.src);
    """)

    # Handy view that maps edge ids to their reversed id
    db.execute("""
        CREATE VIEW flip_edge AS
            SELECT X.id as forward_id, Y.id as reverse_id
            FROM edge X, edge Y
            WHERE X.src = Y.dst AND X.dst = Y.src;
    """)

def maxflow(bfs_max_iterations=float('inf'), flow_max_iterations=float('inf')):
    """
    Computes the max flow in the graph from SOURCE(id=1) to SINK(id=MAX(id)).
    `bfs_max_iterations` and `flow_max_iterations` limit the number of iterations 
    for BFS and maxflow, respectively. Otherwise the algorithm runs until termination.
    """
    # DO NOT EDIT
    flow_i = 0
    # END DO NOT EDIT SECTION

    while True:
        # DO NOT EDIT
        bfs_i = 0
        # END DO NOT EDIT SECTION

        db.execute("DROP TABLE IF EXISTS paths, flow_to_route CASCADE;")

        # Base case for BFS
        db.execute("""
            SELECT array[e1.id] AS path, array[e1.src, e1.dst] AS nodes
            INTO paths
            FROM edge e1
            WHERE e1.src=1 AND capacity != 0;

            CREATE VIEW terminated_paths AS
               SELECT * FROM paths WHERE nodes[array_length(nodes,1)] = (SELECT MAX(id) FROM node);
            """
        )

        # `path_found` tells us whether we've found a path ending at the sink node
        sink_path_found = False

        # Runs BFS until a path ending at node t is found
        while not sink_path_found:

            # Combine the `paths` table with the `edges` table to extend all paths
            # Hints: a JOIN would be helpful here. Also check the documentation to
            # see how array concatenation work in Postgres.
            db.execute("""
                    SELECT ???
                    INTO tmp
                    ???;

                    DROP TABLE IF EXISTS paths CASCADE;
                    ALTER TABLE tmp RENAME TO paths;
                    CREATE VIEW terminated_paths AS
                        SELECT *
                        FROM paths
                        WHERE nodes[array_length(nodes,1)] = (SELECT MAX(id) FROM node);
                    """)

            # If we've exhausted all potential paths and found nothing, we terminate
            db.execute("SELECT * FROM paths")
            if not db.fetchall():  # if the query returns nothing, then return
                return

            db.execute("SELECT * FROM terminated_paths")
            sink_path_found = bool(db.fetchall())

            # DO NOT EDIT
            bfs_i += 1
            if bfs_i >= bfs_max_iterations:
                return
            # END DO NOT EDIT SECTION

        # Choose one of the valid paths as the one to route flow
        db.execute("""CREATE VIEW chosen_route AS 
                      SELECT * FROM terminated_paths ORDER BY path LIMIT 1""")

        # The amount of flow to route is the minimum along the path we found,
        # aka the constraining capacity
        db.execute("""
            WITH path_edges(path_edge) AS (
                SELECT unnest(path) AS path_edge FROM chosen_route
            ),
            constraining_capacity(capacity) AS (
                ???
            )
            SELECT path_edge AS edge_id, (SELECT * FROM constraining_capacity) as flow 
            INTO flow_to_route FROM path_edges;
        """)

        # First, compute the updated capacity of the forward and residual edges in `updates`
        # Then, update the `edges` table
        db.execute("""
            WITH updates(id, new_capacity) AS (
                ???
            )
            UPDATE edge
              SET ???
              FROM updates
              WHERE ???;
            """)

        # DO NOT EDIT
        flow_i += 1
        if flow_i >= flow_max_iterations:
            return
        # END DO NOT EDIT SECTION


def final_flow():
    """
    Computes the actual amount of flow routed on each edge based on the difference
    between current edge capacity and the original edge capacity.
    Returns the amount of flow that reaches the sink node.
    """
    db.execute("DROP TABLE IF EXISTS flow;")

    db.execute("""
       SELECT E.id, E.src, E.dst, (O.capacity - E.capacity) AS flow
       INTO flow
       FROM edge E
       JOIN original_edge O ON E.id = O.id;
     """)
    db.execute("SELECT * FROM flow")
    db.execute("SELECT SUM(flow) FROM flow WHERE dst=(SELECT MAX(id) FROM node)")
    computed_flow = int(db.fetchone()[0])
    return computed_flow

def main():
    p = argparse.ArgumentParser()
    p.add_argument('test')
    p.add_argument('--bfs-max-iterations', metavar='NUM', default=float('inf'), type=int)
    p.add_argument('--flow-max-iterations', metavar='NUM', default=float('inf'), type=int)
    args = p.parse_args()

    setup(args.test)
    maxflow(args.bfs_max_iterations, args.flow_max_iterations)
    final_flow()

if __name__ == "__main__":
    main()
    db.execute("SELECT * FROM flow")
    for x in db.fetchall():
        print x