%
(1001)
(T12 D=1.4 CR=0 - ZMIN=-1 - flat end mill)
G90 G94
G17
G21
G28 G91 Z0
G90

(2D Contour1)
M9
T12 M6
Z10
G54
M8
G0 X13.86 Y29.12
S1000 M3
Z5
G1 Z2 F200
Z-0.86
G19 G2 Y28.98 Z-1 J-0.14
G1 Y28.84
G17 G3 X14 Y28.7 I0.14
G2 Y-0.7 J-14.7
Y28.7 J14.7
G3 X14.14 Y28.84 J0.14
G1 Y28.98
G19 G3 Y29.12 Z-0.86 K0.14
G0 Z10
S0 M5
G17
M9
G28 G91 Z0
G28 X0 Y0
M30

