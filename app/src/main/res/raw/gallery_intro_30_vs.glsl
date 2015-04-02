precision highp float;

out vec2 vTexCoord;

in vec4 aPosition;
in vec2 aTexCoord;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

uniform highp float uPointSize;

void main() {
    vec4 pos = uPMatrix * uVMatrix * uMMatrix * aPosition;

    vTexCoord = aTexCoord;

    gl_PointSize = uPointSize;
    gl_Position = pos;
}
