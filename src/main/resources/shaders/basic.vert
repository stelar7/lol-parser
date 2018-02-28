#version 150 core

uniform mat4 mvp;

in vec3 position;
in vec2 vertexUV;

out vec3 pos;
out vec2 uv;

void main()
{
    gl_Position =  mvp * vec4(position, 1.0);


    // pass on data to the fragment shader
    pos = position;
    uv = vertexUV;
}