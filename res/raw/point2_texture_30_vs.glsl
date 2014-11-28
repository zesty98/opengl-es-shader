#version 300 es

out vec2 vTexCoord;
out float vNormalizedDistFromTouch;

layout(location=0) in vec4 aPosition;
layout(location=1) in vec2 aTexCoord;
layout(location=4) in float aNormalizedDistFromTouch;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

uniform lowp float uPointSize;

void main() {
    vec4 pos = uPMatrix * uVMatrix * uMMatrix * aPosition;

    vTexCoord = aTexCoord;
    vNormalizedDistFromTouch = aNormalizedDistFromTouch;

    gl_PointSize = uPointSize;
    gl_Position = pos;
}

