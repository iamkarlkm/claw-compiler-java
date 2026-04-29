#ifndef OPENCV_CORE_HPP
#define OPENCV_CORE_HPP

#include <cstdint>

#define CV_VERSION "4.5.5"
#define CV_VERSION_INT 4505

// Constants
#define CV_8UC1 0
#define CV_8UC3 16
#define CV_8UC4 24
#define CV_32FC1 5

#define CV_BGR2GRAY 6
#define CV_BGR2RGB 4
#define CV_BGR2HSV 54

// Structs
struct Point {
    int x;
    int y;
};

struct Point2f {
    float x;
    float y;
};

struct Size {
    int width;
    int height;
};

struct Rect {
    int x;
    int y;
    int width;
    int height;
};

struct Scalar {
    double v0;
    double v1;
    double v2;
    double v3;
};

struct Mat;

// Core functions
Mat cv::imread(const char* filename, int flags = 1);
bool cv::imwrite(const char* filename, Mat img);
void cv::imshow(const char* winname, Mat img);
int cv::waitKey(int delay = 0);
void cv::destroyAllWindows();

// Color conversion
Mat cv::cvtColor(Mat src, int code);
Mat cv::toGray(Mat src);
Mat cv::toHSV(Mat src);

// Blur
Mat cv::blur(Mat src, Size ksize);
Mat cv::GaussianBlur(Mat src, Size ksize, double sigma);
Mat cv::Canny(Mat image, double threshold1, double threshold2);

// Threshold
Mat cv::threshold(Mat src, double thresh, double maxval, int type);

// Drawing
void cv::line(Mat img, Point pt1, Point pt2, Scalar color, int thickness = 1);
void cv::rectangle(Mat img, Rect rect, Scalar color, int thickness = 1);
void cv::circle(Mat img, Point center, int radius, Scalar color, int thickness = 1);
void cv::putText(Mat img, const char* text, Point org, int fontFace, double fontScale, Scalar color);

#endif