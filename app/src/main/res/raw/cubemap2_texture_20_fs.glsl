precision mediump float;

varying vec3 vLookupVec;

uniform samplerCube uTexture;

void main() {
    gl_FragColor = textureCube(uTexture, vLookupVec);
}
