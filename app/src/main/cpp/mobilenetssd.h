#ifndef MOB_NET
#define MOB_NET
#ifdef __cplusplus  
extern "C" {  
#endif  
	
struct ai_object_t
{
    int label;
    char *name;
    float prob;
    float x;
    float y;
    float xe;
    float ye;
};
#define OBJMAX  24

int mobilenet_aiinit(void);

int mobilenet_aifini(void);

struct ai_object_t* mobilenet_aidetect(const unsigned char* pixels, int w, int h, int *objcnt);

void mobilenet_yuv420sp2rgb(const unsigned char* yuv420sp, int w, int h, unsigned char* rgb);
#ifdef __cplusplus  
}  
#endif
#endif