#version 300 es

layout(location=0) in vec4 aPosition;
layout(location=1) in vec2 aTexCoord;

out vec4 vColor;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

uniform lowp float uPointSize;

uniform sampler2D uTexture;

void main() {
    vec4 pos = uPMatrix * uVMatrix * uMMatrix * aPosition;

    vColor = texture(uTexture, aTexCoord);;

    gl_PointSize = uPointSize;
    gl_Position = pos;
}
