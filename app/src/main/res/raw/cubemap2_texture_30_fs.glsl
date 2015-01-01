#version 300 es

precision mediump float;

in vec3 vLookupVec;

layout(location=0) out vec4 fragColor;

uniform samplerCube uTexture;

void main() {
    fragColor = texture(uTexture, vLookupVec);
}
