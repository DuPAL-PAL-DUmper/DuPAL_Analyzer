# TODO List
## Board <-> PC communication
- Make the reading of command responses more robust:
    - DuPALManager.readResponse
        - Build a buffer where the response is accumulated until a character that signifies "end of response" (']') is received.