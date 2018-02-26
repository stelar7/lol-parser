#version 150 core

uniform mat4 mvp;

in vec3 position;

out vec3 pos;

void main()
{
    gl_Position =  mvp * vec4(position, 1.0);
    pos = position;
}