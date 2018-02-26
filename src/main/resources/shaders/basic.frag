#version 150 core

out vec4 outColor;
in vec3 pos;

void main()
{
    outColor = vec4(pos, 1.0);
}