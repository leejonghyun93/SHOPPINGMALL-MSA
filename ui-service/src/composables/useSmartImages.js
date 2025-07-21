/**
 * 스마트 이미지 시스템을 위한 Composable
 */
export function useSmartImages() {
    const BASE_IMAGE_PATH = '/images/banners/products/'  // 기본 이미지용
    const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://13.209.253.241:8080'

    const getProductImage = (product) => {
        // 🔥 관리자가 업로드한 이미지 - API Gateway 경로 사용
        if (product.mainImage && product.mainImage.startsWith('/upload/')) {
            // /upload/product/main/파일명.jpg -> /api/images/products/파일명.jpg
            const fileName = product.mainImage.split('/').pop()
            return `${API_BASE_URL}/api/images/products/${fileName}`
        }

        // 🔥 image 필드도 업로드된 파일인 경우
        if (product.image && product.image.startsWith('/upload/')) {
            const fileName = product.image.split('/').pop()
            return `${API_BASE_URL}/api/images/products/${fileName}`
        }

        // 🔥 파일명만 있는 경우 (관리자 업로드)
        if (product.mainImage && !product.mainImage.startsWith('/') && !product.mainImage.startsWith('http')) {
            return `${API_BASE_URL}/api/images/products/${product.mainImage}`
        }

        // 🔥 image 필드가 파일명만 있는 경우
        if (product.image && !product.image.startsWith('/') && !product.image.startsWith('http')) {
            return `${API_BASE_URL}/api/images/products/${product.image}`
        }

        // 이미 완전한 URL인 경우 (http로 시작하는 외부 이미지)
        if (product.image && product.image.startsWith('http')) {
            return product.image
        }

        // 이미 완전한 경로인 경우 (/images로 시작) - 기본 이미지들
        if (product.image && product.image.startsWith('/images')) {
            return product.image
        }

        // mainImage가 파일명만 있는 경우 (레거시)
        if (product.mainImage && !product.mainImage.startsWith('/')) {
            return `${BASE_IMAGE_PATH}${product.mainImage}`
        }

        // image 필드가 파일명만 있는 경우 (레거시)
        if (product.image && !product.image.startsWith('/')) {
            return `${BASE_IMAGE_PATH}${product.image}`
        }

        // 기본 이미지
        return '/images/banners/products/default-product.jpg'
    }

    const handleImageError = (event) => {
        // 첫 번째 실패: 기본 이미지로 교체
        if (!event.target.src.includes('default-product.jpg')) {
            event.target.src = '/images/banners/products/default-product.jpg'
        } else {
            // 기본 이미지도 실패하면 빈 이미지
            event.target.style.display = 'none'
        }
    }

    const handleImageLoad = (event) => {
        // 로딩 완료 시 처리
        event.target.style.opacity = '1'
    }

    return {
        getProductImage,
        handleImageError,
        handleImageLoad
    }
}