#version 150

in vec4 vertPos;
in vec2 textCoordinates;

uniform sampler2D textureSampler;

out vec4 outColor;

void main() {
    outColor.rgb = vec3(vertPos.z/vertPos.w + 1)/2;
} 
