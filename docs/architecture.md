# EduSpark Agent Architecture

The MVP focuses on a durable task state machine, MySQL-backed task traces, and a Server-Sent Events stream that lets the web console observe task progress in real time.

Later phases add Spring AI planning, local Java tools, Milvus hybrid search, and a Critic Agent retry loop without changing the public task lifecycle contract.
