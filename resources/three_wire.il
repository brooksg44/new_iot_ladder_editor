LD %I0.0    ; Stop button (normally closed)
OR %Q0.0    ; Motor seal-in contact
ANDN %I0.1  ; Start button (normally open, inverted logic for stop)
ST %Q0.0    ; Motor output
